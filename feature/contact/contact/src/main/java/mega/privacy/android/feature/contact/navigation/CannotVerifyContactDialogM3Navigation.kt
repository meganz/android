package mega.privacy.android.feature.contact.navigation

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.feature.contact.dialog.CannotVerifyContactDialogM3
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.CannotVerifyContactDialogNavKey

/**
 * Cannot verify contact dialog m3
 *
 * @param remove
 * @param onHandled
 */
fun EntryProviderScope<DialogNavKey>.cannotVerifyContactDialogM3(
    remove: (NavKey) -> Unit,
    onHandled: () -> Unit,
) {
    entry<CannotVerifyContactDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Cannot Verify Contact Dialog"
            )
        )
    ) { key ->
        CannotVerifyContactDialogM3(
            email = key.email,
            onDismiss = {
                remove(key)
                onHandled()
            }
        )
    }
}