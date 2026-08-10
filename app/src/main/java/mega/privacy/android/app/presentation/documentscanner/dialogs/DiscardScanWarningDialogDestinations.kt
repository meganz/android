package mega.privacy.android.app.presentation.documentscanner.dialogs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.DiscardScanWarningDialogNavKey
import mega.privacy.android.shared.nodes.dialog.DiscardScanWarningDialog

data object DiscardScanWarningDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<in DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            discardScanWarningDialogDestination(
                remove = navigationHandler::remove,
                backTo = navigationHandler::backTo,
                onDialogHandled = onHandled,
            )
        }
}

fun EntryProviderScope<in DialogNavKey>.discardScanWarningDialogDestination(
    remove: (NavKey) -> Unit,
    backTo: (NavKey, Boolean) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<DiscardScanWarningDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        DiscardScanWarningDialog(
            hasMultipleScans = key.hasMultipleScans,
            onDiscard = {
                backTo(key.startNavKey, true)
                onDialogHandled()
            },
            onCancel = {
                remove(key)
                onDialogHandled()
            },
        )
    }
}
