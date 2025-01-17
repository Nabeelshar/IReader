package org.ireader.bookDetails.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val COLLAPSED_MAX_LINES = 3

@Composable
fun BookSummaryDescription(
    description: String,
    isExpandable: Boolean?,
    setIsExpandable: (Boolean) -> Unit,
    isExpanded: Boolean,
    onClickToggle: () -> Unit,
) {
    Layout(
        modifier = Modifier.clickable(enabled = isExpandable == true, onClick = onClickToggle),
        measurePolicy = { measurables, constraints ->
            val textPlaceable = measurables.first { it.layoutId == "text" }.measure(constraints)

            if (isExpandable != true) {
                layout(constraints.maxWidth, textPlaceable.height) {
                    textPlaceable.placeRelative(0, 0)
                }
            } else {
                val iconPlaceable = measurables.first { it.layoutId == "icon" }.measure(constraints)

                val layoutHeight = textPlaceable.height +
                    if (isExpanded) iconPlaceable.height else iconPlaceable.height / 2

                val scrimPlaceable = measurables.find { it.layoutId == "scrim" }
                    ?.measure(constraints.copy(maxHeight = layoutHeight / 2))

                layout(constraints.maxWidth, layoutHeight) {
                    textPlaceable.placeRelative(0, 0)
                    scrimPlaceable?.placeRelative(0, layoutHeight - scrimPlaceable.height)
                    iconPlaceable.placeRelative(
                        x = constraints.maxWidth / 2 - iconPlaceable.width / 2,
                        y = layoutHeight - iconPlaceable.height
                    )
                }
            }
        },
        content = {
            androidx.compose.material3.Text(
                text = description,
                modifier = Modifier
                    .padding(horizontal = 0.dp, vertical = 4.dp)
                    .layoutId("text"),
                maxLines = if (!isExpanded) COLLAPSED_MAX_LINES else Int.MAX_VALUE,
                onTextLayout = { result ->
                    if (isExpandable == null || isExpandable == false) {
                        setIsExpandable(result.didOverflowHeight)
                    }
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
            if (isExpandable == true) {
                if (!isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.4f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                    0.5f to MaterialTheme.colorScheme.background
                                )
                            )
                            .layoutId("scrim")
                    )
                }
                IconButton(
                    onClick = onClickToggle,
                    modifier = Modifier.layoutId("icon")
                ) {
                    Icon(
                        if (!isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                        null
                    )
                }
            }
        }
    )
}
