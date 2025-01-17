package org.ireader.image_loader.coil

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import org.ireader.core_api.http.HttpClients
import org.ireader.core_api.http.okhttp
import org.ireader.core_catalogs.CatalogStore
import org.ireader.image_loader.coil.cache.CoverCache
import org.ireader.image_loader.coil.image_loaders.BookCoverFetcher
import org.ireader.image_loader.coil.image_loaders.BookCoverKeyer

class CoilLoaderFactory(
    private val context: Application,
    private val client: HttpClients,
    private val catalogStore: CatalogStore,
    private val coverCache: CoverCache,
) : ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(context).apply {
            val diskCacheInit = { CoilDiskCache.get(context) }
            val callFactoryInit = { client.default.okhttp }
            components {
                add(
                    BookCoverFetcher.Factory(
                        callFactoryLazy = lazy(callFactoryInit),
                        diskCacheLazy = lazy(diskCacheInit),
                        coverCache,
                        catalogStore,
                    )
                )
                add(CatalogRemoteMapper())
                add(CatalogInstalledFetcher.Factory())
                add(BookCoverKeyer())
            }
            crossfade(300)
            diskCache(diskCacheInit)
            callFactory(callFactoryInit)
        }.build()
    }

    /**
     * Direct copy of Coil's internal SingletonDiskCache so that [BookCoverFetcher] can access it.
     */
    internal object CoilDiskCache {

        private const val FOLDER_NAME = "image_cache"
        private var instance: DiskCache? = null

        @Synchronized
        fun get(context: Context): DiskCache {
            return instance ?: run {
                val safeCacheDir = context.cacheDir.apply { mkdirs() }
                // Create the singleton disk cache instance.
                DiskCache.Builder()
                    .directory(safeCacheDir.resolve(FOLDER_NAME))
                    .build()
                    .also { instance = it }
            }
        }
    }
}
