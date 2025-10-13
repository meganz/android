package mega.privacy.android.navigation.contract

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey

interface AppDialogDestinations {
    val navigationGraph: EntryProviderBuilder<NavKey>.(navigationHandler: NavigationHandler, onHandled: () -> Unit) -> Unit
}