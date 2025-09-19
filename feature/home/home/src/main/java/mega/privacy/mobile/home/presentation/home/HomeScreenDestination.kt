package mega.privacy.mobile.home.presentation.home

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object Home : NavKey

fun NavGraphBuilder.homeScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    composable<Home> {
        val viewmodel = hiltViewModel<HomeViewModel>()

        HomeScreen()
    }
}
