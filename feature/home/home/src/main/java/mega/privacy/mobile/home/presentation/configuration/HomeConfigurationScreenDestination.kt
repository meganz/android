package mega.privacy.mobile.home.presentation.configuration

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object HomeConfiguration : NavKey

fun NavGraphBuilder.homeConfigurationScreen(
    navigationHandler: NavigationHandler,
) {
    composable<HomeConfiguration> {
        val viewmodel = hiltViewModel<HomeConfigurationViewModel>()
        val state by viewmodel.state.collectAsStateWithLifecycle()

        HomeConfigurationScreen(
            state = state,
            onWidgetEnabledChange = viewmodel::updateEnabledState,
            onWidgetOrderChange = viewmodel::updateWidgetOrder,
            onDeleteWidget = viewmodel::deleteWidget,
        )
    }
}