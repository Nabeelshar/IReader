package org.ireader.sources.global_search

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ireader.common_models.entities.BaseBook
import org.ireader.common_resources.UiText
import org.ireader.components.list.layouts.BookImage
import org.ireader.components.reusable_composable.AppIconButton
import org.ireader.components.reusable_composable.MidSizeTextComposable
import org.ireader.components.reusable_composable.SmallTextComposable
import org.ireader.core_ui.ui_components.DotsFlashing
import org.ireader.sources.global_search.viewmodel.GlobalSearchState
import org.ireader.sources.global_search.viewmodel.SearchItem
import org.ireader.ui_sources.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    vm: GlobalSearchState,
    onPopBackStack: () -> Unit,
    onSearch: (query: String) -> Unit,
    onBook: (BaseBook) -> Unit,
    onGoToExplore: (Int) -> Unit,

) {

    val uiSearch = vm.searchItems.filter { it.items.isNotEmpty() }
    val emptySearches = vm.searchItems.filter { it.items.isEmpty() }
    val allSearch = uiSearch + emptySearches

    Scaffold(
        topBar = {
            GlobalScreenTopBar(
                onPop = onPopBackStack,
                onSearch = onSearch,
                state = vm
            )
        }
    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .verticalScroll(rememberScrollState()),
//        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
             .padding(padding)) {
                items(count = allSearch.size) { index ->
                    GlobalSearchBookInfo(
                        allSearch[index],
                        onBook = onBook,
                        goToExplore = { onGoToExplore(index) }
                    )
                }
            }
        }
//    }
}


@Composable
fun GlobalSearchBookInfo(
    book: SearchItem,
    onBook: (BaseBook) -> Unit,
    goToExplore: () -> Unit,
) {
    val modifier = when (book.items.isNotEmpty()) {
        true ->
            Modifier
                .fillMaxWidth()
                .animateContentSize()
        else -> Modifier
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.Start
            ) {
                MidSizeTextComposable(text = book.source.name, fontWeight = FontWeight.Bold)
                SmallTextComposable(text = UiText.DynamicString(book.source.lang.uppercase()))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotsFlashing(book.loading)
                AppIconButton(
                    imageVector = Icons.Default.ArrowForward,
                   contentDescription = stringResource(R.string.open_explore),
                    onClick = goToExplore
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        LazyRow(modifier= Modifier) {
            items(book.items.size) { index ->
                BookImage(
                    modifier = Modifier
                        .height(250.dp)
                        .aspectRatio(3f / 4f),
                    onClick = {
                        onBook(it)
                    },
                    book = book.items[index]
                ) {
                }
            }
        }
    }
}
