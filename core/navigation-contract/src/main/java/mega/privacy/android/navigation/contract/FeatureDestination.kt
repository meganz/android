package mega.privacy.android.navigation.contract

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

interface FeatureDestination {
    val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit
}