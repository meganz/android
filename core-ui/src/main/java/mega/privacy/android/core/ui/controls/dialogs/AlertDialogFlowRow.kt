package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * This is a copy of the native internal AlertDialogFlowRow in Material 2 [AlertDialog] to fix a problem with RTL languages.
 * There's a To do task in this native flow row but it looks like it won't be solved
 */
@Composable
internal fun AlertDialogFlowRow(
    mainAxisSpacing: Dp = 8.dp,
    crossAxisSpacing: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        val direction = LocalLayoutDirection.current
        Layout(content) { measurables, constraints ->
            val sequences = mutableListOf<List<Placeable>>()
            val crossAxisSizes = mutableListOf<Int>()
            val crossAxisPositions = mutableListOf<Int>()

            var mainAxisSpace = 0
            var crossAxisSpace = 0

            val currentSequence = mutableListOf<Placeable>()
            var currentMainAxisSize = 0
            var currentCrossAxisSize = 0

            val childConstraints = Constraints(maxWidth = constraints.maxWidth)

            // Return whether the placeable can be added to the current sequence.
            fun canAddToCurrentSequence(placeable: Placeable) =
                currentSequence.isEmpty() || currentMainAxisSize + mainAxisSpacing.roundToPx() +
                        placeable.width <= constraints.maxWidth

            // Store current sequence information and start a new sequence.
            fun startNewSequence() {
                if (sequences.isNotEmpty()) {
                    crossAxisSpace += crossAxisSpacing.roundToPx()
                }
                // Ensures that confirming actions appear above dismissive actions.
                sequences.add(0, currentSequence.toList())
                crossAxisSizes += currentCrossAxisSize
                crossAxisPositions += crossAxisSpace

                crossAxisSpace += currentCrossAxisSize
                mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

                currentSequence.clear()
                currentMainAxisSize = 0
                currentCrossAxisSize = 0
            }

            for (measurable in measurables) {
                // Ask the child for its preferred size.
                val placeable = measurable.measure(childConstraints)

                // Start a new sequence if there is not enough space.
                if (!canAddToCurrentSequence(placeable)) startNewSequence()

                // Add the child to the current sequence.
                if (currentSequence.isNotEmpty()) {
                    currentMainAxisSize += mainAxisSpacing.roundToPx()
                }
                currentSequence.add(placeable)
                currentMainAxisSize += placeable.width
                currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
            }

            if (currentSequence.isNotEmpty()) startNewSequence()

            val mainAxisLayoutSize = if (constraints.maxWidth != Constraints.Infinity) {
                constraints.maxWidth
            } else {
                max(mainAxisSpace, constraints.minWidth)
            }
            val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

            val layoutWidth = mainAxisLayoutSize

            val layoutHeight = crossAxisLayoutSize

            layout(layoutWidth, layoutHeight) {
                sequences.forEachIndexed { i, placeables ->
                    val childrenMainAxisSizes = IntArray(placeables.size) { j ->
                        placeables[j].width +
                                if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                    }
                    val arrangement = Arrangement.End
                    // Handle vertical direction
                    val mainAxisPositions = IntArray(childrenMainAxisSizes.size) { 0 }
                    with(arrangement) {
                        arrange(
                            mainAxisLayoutSize,
                            childrenMainAxisSizes,
                            direction,
                            mainAxisPositions
                        )
                    }
                    placeables.forEachIndexed { j, placeable ->
                        placeable.place(
                            x = mainAxisPositions[j],
                            y = crossAxisPositions[i]
                        )
                    }
                }
            }
        }
    }
}