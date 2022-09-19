package mega.privacy.android.app.presentation.settings.startSceen.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Start screen option
 *
 * @property startScreen
 * @property title
 * @property icon
 */
data class StartScreenOption(
    val startScreen: StartScreen,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
)
