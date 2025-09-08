package mega.privacy.android.app.mediaplayer.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Navigation bar insets for different positions
 *
 * @property bottom Bottom inset
 * @property left Left inset
 * @property right Right inset
 */
data class NavigationBarInsets(
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp,
    val right: Dp = 0.dp,
)
