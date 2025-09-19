package mega.privacy.mobile.home.presentation.configuration

import androidx.hilt.navigation.compose.hiltViewModel
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

        HomeConfigurationScreen()
    }
}