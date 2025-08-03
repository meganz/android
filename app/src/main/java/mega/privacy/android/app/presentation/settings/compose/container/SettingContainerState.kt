package mega.privacy.android.app.presentation.settings.compose.container

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.navigation.contract.NavigationHandler

class SettingContainerState(
    val nestedGraphs: List<NavGraphBuilder.(navigationHandler: NavigationHandler) -> Unit>
)
