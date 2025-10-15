package mega.privacy.android.core.nodecomponents.dialog.contact

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.Serializable

@Serializable
data class CannotVerifyContactDialogNavKey(val email: String) : NavKey

fun EntryProviderScope<NavKey>.cannotVerifyContactDialogM3(
    onBack: () -> Unit,
) {
    entry<CannotVerifyContactDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Cannot Verify Contact Dialog"
            )
        )
    ) { key ->
        CannotVerifyContactDialogM3(email = key.email, onDismiss = onBack)
    }
}