package mega.privacy.android.app.mediaplayer.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Navigation bar and display cutout insets for different positions
 *
 * @property bottom Bottom navigation bar inset
 * @property left Left navigation bar inset
 * @property right Right navigation bar inset
 * @property cutoutLeft Left display cutout inset
 * @property cutoutRight Right display cutout inset
 */
data class NavigationBarInsets(
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp,
    val right: Dp = 0.dp,
    val cutoutLeft: Dp = 0.dp,
    val cutoutRight: Dp = 0.dp,
)
