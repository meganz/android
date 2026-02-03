package mega.privacy.mobile.home.presentation.home

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.mobile.analytics.event.HomeScreenEvent

@Serializable
data object Home : MainNavItemNavKey

fun EntryProviderScope<NavKey>.homeScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<Home>(
        metadata = buildMetadata {
            withScreenViewEvent(HomeScreenEvent)
        }
    ) {
        val viewmodel = hiltViewModel<HomeViewModel>()
        val state by viewmodel.state.collectAsStateWithLifecycle()
        HomeScreen(
            state = state,
            navigationHandler = navigationHandler,
            transferHandler = transferHandler,
        )
    }
}
