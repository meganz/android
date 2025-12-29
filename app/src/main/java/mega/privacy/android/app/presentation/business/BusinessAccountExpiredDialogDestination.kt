package mega.privacy.android.app.presentation.business

import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.privacy.android.app.R
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.BusinessAccountExpiredDialogNavKey
import mega.privacy.android.shared.resources.R as sharedR

data object BusinessAccountExpiredDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            businessAccountExpiredDialogDestination(
                remove = navigationHandler::remove,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.businessAccountExpiredDialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<BusinessAccountExpiredDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val titleRes = if (key.isProFlexiAccount) {
            sharedR.string.account_pro_flexi_account_deactivated_dialog_title
        } else {
            R.string.account_business_account_deactivated_dialog_title
        }

        val messageRes = when {
            key.isMasterBusinessAccount -> {
                R.string.account_business_account_deactivated_dialog_admin_body
            }

            key.isProFlexiAccount -> {
                sharedR.string.account_pro_flexi_account_deactivated_dialog_body
            }

            else -> {
                R.string.account_business_account_deactivated_dialog_sub_user_body
            }
        }

        BasicDialog(
            title = stringResource(id = titleRes),
            description = stringResource(id = messageRes),
            buttons = persistentListOf(
                BasicDialogButton(
                    text = stringResource(id = R.string.account_business_account_deactivated_dialog_button),
                    onClick = {
                        onDialogHandled()
                        remove(key)
                    }
                ),
            ),
            onDismissRequest = {
                onDialogHandled()
                remove(key)
            },
        )
    }
}

