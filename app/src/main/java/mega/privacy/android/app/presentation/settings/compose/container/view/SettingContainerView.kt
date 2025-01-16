package mega.privacy.android.app.presentation.settings.compose.container.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.compose.container.SettingContainerState
import mega.privacy.android.app.presentation.settings.compose.home.navigation.SettingsHomeGraph
import mega.privacy.android.app.presentation.settings.compose.home.navigation.settingsHomeGraph

@Composable
internal fun SettingContainerView(
    state: SettingContainerState,
    initialKey: String?,
    navHostController: NavHostController,
    getTitle: (NavBackStackEntry?) -> String?,
    onNavigateBack: () -> Unit,
) {
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()

    val title = getTitle(navBackStackEntry) ?: stringResource(
        R.string.action_settings
    )
    val nestedController = rememberNavController()

    MegaScaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back {
                    if (nestedController.navigateUp().not()) onNavigateBack()
                },
                title = title,
            )
        },
        snackbarHost = {},
        bottomBar = {},
        content = { padding ->
            NavHost(
                navController = nestedController,
                startDestination = SettingsHomeGraph,
                modifier = Modifier.padding(padding)
            ) {
                settingsHomeGraph(
                    navController = nestedController,
                    initialSetting = initialKey,
                )
                state.nestedGraphs.forEach {
                    it(
                        this,
                        navHostController,
                    )
                }
            }
        }
    )
}
