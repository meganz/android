package mega.privacy.android.core.passcode.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.core.passcode.presentation.model.PasscodeCryptObjectFactory
import mega.privacy.android.core.passcode.presentation.view.PasscodeView

@Serializable
data object PasscodeNavKey : NavKey

fun EntryProviderScope<NavKey>.passcodeView(
    cryptObjectFactory: PasscodeCryptObjectFactory,
    logoutConfirmationDialog: @Composable (onDismissed: () -> Unit) -> Unit = {},
) {
    entry<PasscodeNavKey> {
        PasscodeView(
            cryptObjectFactory = cryptObjectFactory,
            logoutConfirmationDialog = logoutConfirmationDialog,
        )
    }
}
