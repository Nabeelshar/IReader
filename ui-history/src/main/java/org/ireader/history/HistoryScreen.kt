package org.ireader.history

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ireader.common_models.entities.HistoryWithRelations
import org.ireader.common_resources.UiText
import org.ireader.core_ui.ui.EmptyScreen
import org.ireader.core_ui.ui.LoadingScreen
import org.ireader.history.viewmodel.HistoryState

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    state: HistoryState,
    onHistory: (HistoryWithRelations) -> Unit,
    onHistoryDelete: (HistoryWithRelations) -> Unit,
    onHistoryPlay: (HistoryWithRelations) -> Unit,
    onBookCover: (HistoryWithRelations) -> Unit,
) {

    Box(modifier = modifier) {
        Crossfade(targetState = Pair(state.isLoading, state.isEmpty)) { (isLoading, isEmpty) ->
            when {
                isLoading -> LoadingScreen()
                isEmpty -> EmptyScreen(text = UiText.DynamicString("Nothing read recently"))
                else -> HistoryContent(
                    state = state,
                    onClickItem = onHistory,
                    onClickDelete = onHistoryDelete,
                    onClickPlay = onHistoryPlay,
                    onBookCover = onBookCover
                )
            }
        }
    }
}
