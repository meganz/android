package mega.privacy.android.app.main.ads

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AdsFreeIntroNavKey
import mega.privacy.android.shared.ads.adsfreeintro.AdsFreeIntroScreen

fun EntryProviderScope<NavKey>.adsFreeIntroScreen(
    navigationHandler: NavigationHandler,
) {
    entry<AdsFreeIntroNavKey> {
        AdsFreeIntroScreen(
            onDismiss = { navigationHandler.remove(AdsFreeIntroNavKey) },
        )
    }
}
