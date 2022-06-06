package org.ireader.image_loader.coil.image_loaders

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.network.HttpException
import coil.request.Options
import coil.request.Parameters
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import org.ireader.core_api.http.okhttp
import org.ireader.core_api.source.HttpSource
import org.ireader.core_catalogs.CatalogStore
import org.ireader.image_loader.BookCover
import org.ireader.image_loader.coil.cache.CoverCache
import org.ireader.image_loader.coil.image_loaders.BookCoverFetcher.Companion.USE_CUSTOM_COVER
import java.io.File
import java.net.HttpURLConnection

/**
 * A [Fetcher] that fetches cover image for [BookCover] object.
 *
 * It uses [BookCover.c] if custom cover is not set by the user.
 * Disk caching for library items is handled by [CoverCache], otherwise
 * handled by Coil's [DiskCache].
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER]: Use custom cover if set by user, default is true
 */
class BookCoverFetcher(
    private val manga: BookCover,
    private val sourceLazy: Lazy<HttpSource?>,
    private val options: Options,
    private val coverCache: CoverCache,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val diskCacheLazy: Lazy<DiskCache>,
) : Fetcher {

    // For non-custom cover
    private val diskCacheKey: String? by lazy { BookCoverKeyer().key(manga, options) }
    private lateinit var url: String

    override suspend fun fetch(): FetchResult {
        // Use custom cover if exists
        val useCustomCover = options.parameters.value(USE_CUSTOM_COVER) ?: true
        val customCoverFile = coverCache.getCustomCoverFile(manga)
        if (useCustomCover && customCoverFile.exists()) {
            return fileLoader(customCoverFile)
        }

        // diskCacheKey is thumbnail_url
        url = diskCacheKey ?: error("No cover specified")
        return when (getResourceType(url)) {
            Type.URL -> httpLoader()
            Type.File -> fileLoader(File(url.substringAfter("file://")))
            null -> error("Invalid image")
        }
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), diskCacheKey = diskCacheKey),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun httpLoader(): FetchResult {
        // Only cache separately if it's a library item
        val libraryCoverCacheFile = if (manga.favorite) {
            coverCache.getCoverFile(manga) ?: error("No cover specified")
        } else {
            null
        }
        if (libraryCoverCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
            return fileLoader(libraryCoverCacheFile)
        }

        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, libraryCoverCacheFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    return fileLoader(snapshotCoverCache)
                }

                // Read from snapshot
                return SourceResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(response, libraryCoverCacheFile)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(snapshot, response)
                if (snapshot != null) {
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceResult(
                    source = ImageSource(source = responseBody.source(), context = options.context),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.closeQuietly()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.closeQuietly()
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = sourceLazy.value?.client?.okhttp ?: callFactoryLazy.value
        val response = client.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            response.body?.closeQuietly()
            throw HttpException(response)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url)
            .headers(sourceLazy.value?.getCoverRequest(url)?.second?.build()?.convertToOkHttpRequest()?.headers ?: options.headers)
            // Support attaching custom data to the network request.
            .tag(Parameters::class.java, options.parameters)

        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            diskCacheLazy.value.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey!!)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
           org.ireader.core_api.log.Log.error { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            org.ireader.core_api.log.Log.error { "Failed to write response data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) diskCacheLazy.value[diskCacheKey!!] else null
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun writeToDiskCache(
        snapshot: DiskCache.Snapshot?,
        response: Response,
    ): DiskCache.Snapshot? {
        if (!options.diskCachePolicy.writeEnabled) {
            snapshot?.closeQuietly()
            return null
        }
        val editor = if (snapshot != null) {
            snapshot.closeAndEdit()
        } else {
            diskCacheLazy.value.edit(diskCacheKey!!)
        } ?: return null
        try {
            diskCacheLazy.value.fileSystem.write(editor.data) {
                response.body!!.source().readAll(this)
            }
            return editor.commitAndGet()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(file = data, diskCacheKey = diskCacheKey, closeable = this)
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            else -> null
        }
    }

    private enum class Type {
        File, URL
    }

    class Factory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
        private val catalogStore: CatalogStore,
        private val coverCache: CoverCache,
    ) : Fetcher.Factory<BookCover> {


        override fun create(data: BookCover, options: Options, imageLoader: ImageLoader): Fetcher {
            val source = lazy { catalogStore.get(data.sourceId)?.source as? HttpSource }
            return BookCoverFetcher(data, source, options, coverCache, callFactoryLazy, diskCacheLazy)
        }
    }

    companion object {
        const val USE_CUSTOM_COVER = "use_custom_cover"

        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE = CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}