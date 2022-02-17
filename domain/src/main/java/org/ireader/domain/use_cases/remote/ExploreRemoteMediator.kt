package org.ireader.domain.use_cases.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import org.ireader.domain.local.BookDatabase
import org.ireader.domain.models.ExploreType
import org.ireader.domain.models.RemoteKeys
import org.ireader.domain.models.entities.Book
import org.ireader.domain.models.entities.FilterList
import org.ireader.domain.models.source.Source
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

@ExperimentalPagingApi
class ExploreRemoteMediator(
    private val source: Source,
    private val database: BookDatabase,
    private val exploreType: ExploreType,
    private val query: String? = null,
) : RemoteMediator<Int, Book>() {

    private val remoteKey = database.remoteKeysDao
    private val chapterDao = database.libraryChapterDao
    private val libraryDao = database.libraryBookDao
    private val Localbook = database.libraryBookDao


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Book>,
    ): RemoteMediator.MediatorResult {
        return try {

            val currentPage = when (loadType) {
                LoadType.REFRESH -> {
                    chapterDao.deleteNotInLibraryChapters()
                    libraryDao.deleteNotInLibraryBooks()
                    val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                    remoteKeys?.nextPage?.minus(1) ?: 1
                }
                LoadType.PREPEND -> {
                    val remoteKeys = getRemoteKeyForFirstItem(state)
                    val prevPage = remoteKeys?.prevPage
                        ?: return RemoteMediator.MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null
                        )
                    prevPage
                }
                LoadType.APPEND -> {
                    val remoteKeys = getRemoteKeyForLastItem(state)
                    val nextPage = remoteKeys?.nextPage
                        ?: return RemoteMediator.MediatorResult.Success(
                            endOfPaginationReached = remoteKeys != null
                        )
                    nextPage
                }
            }

            val response = when (exploreType) {
                is ExploreType.Latest -> {
                    source.getLatest(currentPage)
                }
                is ExploreType.Popular -> {
                    source.getPopular(currentPage)
                }
                is ExploreType.Search -> {
                    source.getSearch(currentPage, query = query ?: "", filters = FilterList())
                }
            }


            val endOfPaginationReached = !response.hasNextPage


            val prevPage = if (currentPage == 1) null else currentPage - 1
            val nextPage = if (endOfPaginationReached) null else currentPage + 1

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKey.deleteAllExploredBook()
                    remoteKey.deleteAllRemoteKeys()
                }
                val keys = response.books.map { book ->
                    RemoteKeys(
                        id = book.title,
                        prevPage = prevPage,
                        nextPage = nextPage,
                        sourceId = source.sourceId
                    )
                }

                remoteKey.addAllRemoteKeys(remoteKeys = keys)
                remoteKey.insertAllExploredBook(response.books.map {
                    it.copy(
                        dataAdded = System.currentTimeMillis(),
                    )
                })
            }
            RemoteMediator.MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: UnknownHostException) {
            return RemoteMediator.MediatorResult.Error(Exception("There is no internet available,please check your internet connection"))
        } catch (e: IOException) {
            return RemoteMediator.MediatorResult.Error(e)
        } catch (e: HttpException) {
            return RemoteMediator.MediatorResult.Error(e)
        } catch (e: SSLHandshakeException) {
            return RemoteMediator.MediatorResult.Error(e)
        } catch (e: Exception) {
            return RemoteMediator.MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, Book>,
    ): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.title?.let { bookName ->
                remoteKey.getRemoteKeys(id = bookName)
            }
        }
    }

    private suspend fun getRemoteKeyForFirstItem(
        state: PagingState<Int, Book>,
    ): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { book ->
                remoteKey.getRemoteKeys(id = book.title)
            }
    }

    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, Book>,
    ): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { book ->
                remoteKey.getRemoteKeys(id = book.title)
            }
    }

}