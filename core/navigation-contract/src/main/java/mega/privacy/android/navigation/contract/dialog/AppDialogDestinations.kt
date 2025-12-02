package mega.privacy.android.navigation.contract.dialog

import androidx.navigation3.runtime.EntryProviderScope
import mega.privacy.android.navigation.contract.NavigationHandler

interface AppDialogDestinations {
    val navigationGraph: EntryProviderScope<DialogNavKey>.(navigationHandler: NavigationHandler, onHandled: () -> Unit) -> Unit
}