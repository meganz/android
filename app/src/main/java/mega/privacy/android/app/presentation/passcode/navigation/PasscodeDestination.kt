package mega.privacy.android.app.presentation.passcode.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.passcode.view.PasscodeView

@Serializable
data object PasscodeNavKey : NavKey

internal fun EntryProviderScope<NavKey>.passcodeView(cryptObjectFactory: PasscodeCryptObjectFactory) {
    entry<PasscodeNavKey> {
        PasscodeView(cryptObjectFactory = cryptObjectFactory)
    }
}