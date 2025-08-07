package mega.privacy.android.app.presentation.settings.startscreen.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Start screen option
 *
 * @property startScreen
 * @property title
 * @property icon
 */
data class StartScreenOption<T>(
    val startScreen: T,
    @StringRes val title: Int,
    val icon: ImageVector,
)
