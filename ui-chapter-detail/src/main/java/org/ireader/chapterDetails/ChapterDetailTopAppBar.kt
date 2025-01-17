package org.ireader.chapterDetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.ireader.chapterDetails.viewmodel.ChapterDetailState
import org.ireader.components.components.Toolbar
import org.ireader.components.reusable_composable.AppIconButton
import org.ireader.components.reusable_composable.BigSizeTextComposable
import org.ireader.ui_chapter_detail.R

@Composable
fun ChapterDetailTopAppBar(
    state: ChapterDetailState,
    onClickCancelSelection: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickFlipSelection: () -> Unit,
    onSelectBetween:() -> Unit,
    onReverseClick: () -> Unit,
    onPopBackStack: () -> Unit,
    onMap: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        when {
            state.hasSelection -> {
                EditModeChapterDetailTopAppBar(
                    selectionSize = state.selection.size,
                    onClickCancelSelection = onClickCancelSelection,
                    onClickSelectAll = onClickSelectAll,
                    onClickInvertSelection = onClickFlipSelection,
                    onSelectBetween = onSelectBetween
                )
            }
            else -> {
                RegularChapterDetailTopAppBar(
                    onReverseClick = onReverseClick,
                    onPopBackStack = onPopBackStack,
                    onMap = onMap
                )
            }
        }
    }
}

@Composable
fun RegularChapterDetailTopAppBar(
    onReverseClick: () -> Unit,
    onPopBackStack: () -> Unit,
    onMap: () -> Unit,
) {
//    CenterAlignedTopAppBar(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(86.dp),
//        title = {
//            BigSizeTextComposable(text = stringResource(R.string.content))
//        },
//        actions = {
//            AppIconButton(imageVector = Icons.Filled.Place, text =  UiText.StringResource(R.string.find_current_chapter), onClick = onMap)
//            IconButton(onClick = onReverseClick) {
//                Icon(
//                    imageVector = Icons.Default.Sort,
//                   contentDescription = stringResource(R.string.sort).asString()
//                )
//            }
//        },
//        navigationIcon = {
//            IconButton(onClick = onPopBackStack) {
//                Icon(
//                    imageVector = Icons.Default.ArrowBack,
//                    contentDescription =  UiText.StringResource(R.string.return_to_previous_screen).asString()
//                )
//            }
//        },
//        colors =  TopAppBarDefaults.centerAlignedTopAppBarColors(
//
//        ),
//    )
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            BigSizeTextComposable(text = stringResource(R.string.content))
        },
        actions = {
            AppIconButton(imageVector = Icons.Filled.Place, contentDescription = stringResource(R.string.find_current_chapter), onClick = onMap)
            AppIconButton(imageVector =Icons.Default.Sort ,contentDescription = stringResource(R.string.sort), onClick = onReverseClick)
        },
        navigationIcon = {
            AppIconButton(imageVector =Icons.Default.ArrowBack , contentDescription = stringResource(R.string.return_to_previous_screen), onClick = onPopBackStack)
        },
    )
}

@Composable
private fun EditModeChapterDetailTopAppBar(
    selectionSize: Int,
    onClickCancelSelection: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onSelectBetween: () -> Unit,
) {
    Toolbar(
        title = { BigSizeTextComposable(text = "$selectionSize") },
        navigationIcon = {
            AppIconButton(imageVector =Icons.Default.Close ,contentDescription = stringResource(R.string.close), onClick = onClickCancelSelection)
        },
        actions = {
            AppIconButton(imageVector =Icons.Default.SelectAll ,contentDescription = stringResource(R.string.select_all), onClick = onClickSelectAll)
            AppIconButton(imageVector =Icons.Default.FlipToBack ,contentDescription = stringResource(R.string.select_inverted), onClick = onClickInvertSelection)
            AppIconButton(imageVector =Icons.Default.SyncAlt ,contentDescription = stringResource(R.string.select_between), onClick = onSelectBetween)
        }
    )
}
