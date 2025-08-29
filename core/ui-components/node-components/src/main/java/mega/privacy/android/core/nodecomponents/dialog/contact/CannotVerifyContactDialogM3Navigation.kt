package mega.privacy.android.core.nodecomponents.dialog.contact

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CannotVerifyContactDialogArgs(val email: String) : NavKey

fun NavGraphBuilder.cannotVerifyContactDialogM3(
    onBack: () -> Unit,
) {
    dialog<CannotVerifyContactDialogArgs> {
        val args = it.toRoute<CannotVerifyContactDialogArgs>()

        CannotVerifyContactDialogM3(email = args.email, onDismiss = onBack)
    }
}