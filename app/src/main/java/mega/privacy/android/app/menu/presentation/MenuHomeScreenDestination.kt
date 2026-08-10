package mega.privacy.android.app.menu.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

@Serializable
data object MenuHomeScreen : MainNavItemNavKey

fun EntryProviderScope<NavKey>.menuHomeScreen(onNavigate: (NavKey) -> Unit) {
    entry<MenuHomeScreen> {
        MenuHomeScreen(
            navigateToFeature = { onNavigate(it) },
        )
    }
}