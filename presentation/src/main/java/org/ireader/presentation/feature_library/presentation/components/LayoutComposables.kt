package org.ireader.presentation.feature_library.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import org.ireader.domain.models.LayoutType
import org.ireader.domain.models.entities.Book
import org.ireader.domain.models.source.Source
import org.ireader.presentation.presentation.layouts.CompactGridLayoutComposable
import org.ireader.presentation.presentation.layouts.GridLayoutComposable
import org.ireader.presentation.presentation.layouts.LinearListDisplay
import org.ireader.presentation.ui.BookDetailScreenSpec


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LayoutComposable(
    navController: NavController,
    books: LazyPagingItems<Book>,
    layout: LayoutType,
    scrollState: LazyListState,
    gridState: LazyGridState,
    source: Source? = null,
    isLocal: Boolean,
    goToLatestChapter: (book: Book) -> Unit = {},
) {

    when (layout) {
        is LayoutType.GridLayout -> {
            GridLayoutComposable(books = books,
                onClick = { book ->
                    navController.navigate(
                        route = BookDetailScreenSpec.buildRoute(sourceId = if (source?.sourceId != null) source.sourceId else book.sourceId,
                            bookId = book.id)
                    )
                }, scrollState = gridState,
                isLocal = isLocal,
                goToLatestChapter = { goToLatestChapter(it) })
        }
        is LayoutType.ListLayout -> {
            LinearListDisplay(books = books, onClick = { book ->
                navController.navigate(
                    route = BookDetailScreenSpec.buildRoute(sourceId = if (source?.sourceId != null) source.sourceId else book.sourceId,
                        bookId = book.id)
                )
            }, scrollState = scrollState,
                isLocal = isLocal,
                goToLatestChapter = { goToLatestChapter(it) })
        }
        is LayoutType.CompactGrid -> {
            CompactGridLayoutComposable(
                books = books,
                onClick = { book ->
                    navController.navigate(
                        route = BookDetailScreenSpec.buildRoute(sourceId = if (source?.sourceId != null) source.sourceId else book.sourceId,
                            bookId = book.id)
                    )
                }, scrollState = gridState,
                isLocal = isLocal,
                goToLatestChapter = { goToLatestChapter(it) })

        }
    }
}