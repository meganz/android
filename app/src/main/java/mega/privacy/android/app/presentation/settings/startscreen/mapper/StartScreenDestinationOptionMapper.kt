package mega.privacy.android.app.presentation.settings.startscreen.mapper

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOption
import mega.privacy.android.navigation.contract.MainNavItem
import javax.inject.Inject

class StartScreenDestinationOptionMapper @Inject constructor() {
    operator fun invoke(mainNavItem: MainNavItem): StartScreenOption<NavKey> {
        return StartScreenOption(
            startScreen = mainNavItem.destination,
            title = mainNavItem.label,
            icon = mainNavItem.icon,
        )
    }
}