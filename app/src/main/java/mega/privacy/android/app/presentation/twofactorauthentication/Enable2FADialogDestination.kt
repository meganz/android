package mega.privacy.android.app.presentation.twofactorauthentication

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.app.R
import mega.privacy.android.core.sharedcomponents.dialog.Enable2FADialogView
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.Enable2FANavKey

data object Enable2FADialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            enable2FADialogDestination(
                remove = navigationHandler::remove,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.enable2FADialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<Enable2FANavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        )
    ) { key ->
        val context = LocalContext.current
        Enable2FADialogView(
            titleText = stringResource(id = R.string.title_enable_2fa),
            descriptionText = stringResource(id = R.string.two_factor_authentication_explain),
            imageRes = R.drawable.ic_2fa,
            enableButtonText = stringResource(id = R.string.general_enable),
            skipButtonText = stringResource(id = R.string.general_skip),
            onDismissRequest = {
                onDialogHandled()
                remove(key)
            },
            onEnable2FA = {
                val intent = Intent(context, TwoFactorAuthenticationActivity::class.java)
                intent.putExtra(ExtraConstant.EXTRA_NEW_ACCOUNT, true)
                context.startActivity(intent)
                onDialogHandled()
                remove(key)
            }
        )
    }
}
