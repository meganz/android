package mega.privacy.android.navigation.contract

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

interface AppDialogDestinations {
    val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, onHandled: () -> Unit) -> Unit
}