package mega.privacy.android.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * Convert an Int pixel value to Dp.
 */
@Composable
fun intToDp(px: Int) = with(LocalDensity.current) { px.toDp() }