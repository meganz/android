package mega.privacy.android.core.passcode.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.core.passcode.presentation.model.PasscodeCryptObjectFactory
import mega.privacy.android.core.passcode.presentation.view.PasscodeView
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression

@Serializable
data object PasscodeNavKey : NavKey

fun EntryProviderScope<NavKey>.passcodeView(
    cryptObjectFactory: PasscodeCryptObjectFactory,
    logoutConfirmationDialog: @Composable (onDismissed: () -> Unit) -> Unit = {},
) {
    entry<PasscodeNavKey>(
        metadata = buildMetadata { withOverlaySuppression() }
    ) {
        PasscodeView(
            cryptObjectFactory = cryptObjectFactory,
            logoutConfirmationDialog = logoutConfirmationDialog,
        )
    }
}
