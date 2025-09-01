package mega.privacy.android.app.presentation.settings.startscreen.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.navigation.contract.MainNavItem
import javax.inject.Inject

class StartScreenSummaryMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(startScreen: StartScreen): String =
        context.resources.getStringArray(R.array.settings_start_screen)[startScreen.id]

    operator fun invoke(mainNavItem: MainNavItem): String =
        context.resources.getString(mainNavItem.label)
}
