package mega.privacy.android.shared.original.core.ui.controls.sheets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.shape.RoundedCornerShapeWithOffset

/**
 * Creates a [RoundedCornerShapeWithOffset] for the bottom sheet with rounded corners and an offset
 * to account for status bar insets.
 *
 * @param cornerRadius The radius of the corners. Defaults to 24.dp.
 * @param applyStatusBarInsetsPadding Whether to apply padding for status bar window insets.
 *        When true make sure to use statusBarsPadding() in bottom sheet content view
 */
@Composable
fun BottomSheetRoundedShape(
    cornerRadius: Dp = 24.dp,
    applyStatusBarInsetsPadding: Boolean = true,
) = RoundedCornerShapeWithOffset(
    offset = Offset(
        x = 0f,
        y = if (applyStatusBarInsetsPadding) WindowInsets.statusBars.getTop(LocalDensity.current)
            .toFloat() else 0f
    ),
    topStart = cornerRadius,
    topEnd = cornerRadius
)
