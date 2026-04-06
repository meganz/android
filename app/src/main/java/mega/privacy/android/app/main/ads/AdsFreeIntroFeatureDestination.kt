package mega.privacy.android.app.main.ads

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import javax.inject.Inject

class AdsFreeIntroFeatureDestination @Inject constructor() : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            adsFreeIntroScreen(navigationHandler = navigationHandler)
        }
}
