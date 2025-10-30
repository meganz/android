package mega.privacy.android.core.sharedcomponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
fun Modifier.selectedBorder(
    isSelected: Boolean,
    shape: Shape = DSTokens.shapes.extraSmall,
    strokeWidth: Dp = 2.dp
): Modifier = if (isSelected) {
    border(
        border = BorderStroke(
            width = strokeWidth,
            color = DSTokens.colors.border.strongSelected
        ),
        shape = shape
    )
} else {
    this
}