package mega.privacy.android.navigation.contract

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey

interface FeatureDestination {
    val navigationGraph: EntryProviderBuilder<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit
}