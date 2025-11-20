package mega.privacy.android.app.presentation.login.logoutdialog

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.LOGGED_OUT_DIALOG
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data object RemoteLogoutDialogNavKey : NoSessionNavKey.Mandatory

data object RemoteLogoutDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            remoteLogoutDestination(
                navigateBack = navigationHandler::back,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<NavKey>.remoteLogoutDestination(
    navigateBack: () -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<RemoteLogoutDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) {
        BasicDialog(
            modifier = Modifier.testTag(LOGGED_OUT_DIALOG),
            title = stringResource(id = R.string.title_alert_logged_out),
            description = stringResource(id = R.string.error_server_expired_session),
            buttons = persistentListOf(
                BasicDialogButton(
                    text = stringResource(id = R.string.general_ok),
                    onClick = {
                        onDialogHandled()
                        navigateBack()
                    }
                ),
            ),
            onDismissRequest = {
                onDialogHandled()
                navigateBack()
            },
        )
    }
}

