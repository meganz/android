package mega.privacy.android.core.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Returns current screen orientation
 * @return true if orientation is in landscape false otherwise
 */
@Composable
fun isScreenOrientationLandscape() =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE