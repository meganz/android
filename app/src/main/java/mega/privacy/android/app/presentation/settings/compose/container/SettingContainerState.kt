package mega.privacy.android.app.presentation.settings.compose.container

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

class SettingContainerState(
    val nestedGraphs: List<NavGraphBuilder.(navHostController: NavHostController) -> Unit>
)
