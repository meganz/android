package mega.privacy.android.core.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Convert Dp to Sp.
 */
@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }