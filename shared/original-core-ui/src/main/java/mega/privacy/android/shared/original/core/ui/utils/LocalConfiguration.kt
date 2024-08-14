package mega.privacy.android.shared.original.core.ui.utils

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

/**
 * Check if device is tablet or not
 * @return true if device is tablet false otherwise
 */
@Composable
fun isTablet() =
    ((LocalConfiguration.current.screenLayout
            and Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE)
