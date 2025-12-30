package mega.privacy.android.core.sharedcomponents.extension

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Extension function to exclude bottom padding from PaddingValues
 */
@Composable
fun PaddingValues.excludingBottomPadding(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        top = calculateTopPadding(),
        start = calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection)
    )
}
