package mega.privacy.android.app.appstate.content.navigation.model

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableSet
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem

@Stable
sealed interface MainNavState {
    data object Loading : MainNavState

    data class Data(
        val mainNavItems: ImmutableSet<NavigationItem>,
        val mainNavScreens: ImmutableSet<EntryProviderBuilder<NavKey>.(navigationHandler: NavigationHandler, NavigationUiController, TransferHandler) -> Unit>,
        val initialDestination: NavKey,
    ) : MainNavState
}