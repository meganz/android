package mega.privacy.android.app.presentation.fingerprintauth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.SecurityUpgradeDialogNavKey

data object SecurityUpgradeDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            securityUpgradeDialogDestination(
                remove = navigationHandler::remove,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.securityUpgradeDialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<SecurityUpgradeDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        SecurityUpgradeDialogView(
            onDismiss = {
                onDialogHandled()
                remove(key)
            },
        )
    }
}

