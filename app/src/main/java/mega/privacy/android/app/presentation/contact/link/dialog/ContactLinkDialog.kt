package mega.privacy.android.app.presentation.contact.link.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mega.android.core.ui.components.dialogs.BasicContactDialog
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.contacts.getAction
import mega.privacy.android.app.presentation.extensions.contacts.getMessage
import mega.privacy.android.app.presentation.extensions.contacts.getNavigation
import mega.privacy.android.navigation.contract.queue.snackbar.snackbarEventQueue
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun ContactLinkDialog(
    uiState: ContactLinkDialogUiState,
    inviteContact: () -> Unit,
    viewContact: () -> Unit,
    onDismiss: () -> Unit,
    navigateToContactRequests: (ContactsNavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    with(uiState) {
        contactLinkQueryResult?.let { result ->
            with(result) {
                BasicContactDialog(
                    modifier = modifier,
                    title = stringResource(
                        if (isContact) {
                            sharedR.string.contact_found_dialog_title
                        } else {
                            sharedR.string.general_invite_contact
                        }
                    ),
                    description = if (isContact) {
                        fullName?.let {
                            stringResource(
                                R.string.context_contact_already_exists,
                                it
                            )
                        }
                    } else {
                        null
                    },
                    contactName = fullName,
                    contactEmail = email,
                    contactAvatarFile = avatarFile,
                    contactAvatarColor = avatarColor,
                    positiveButtonText = stringResource(
                        if (isContact) {
                            R.string.contact_view
                        } else {
                            sharedR.string.invite_contacts_action_label
                        }
                    ),
                    onPositiveButtonClicked = {
                        if (isContact) {
                            onDismiss()
                            viewContact()
                        } else {
                            inviteContact()
                        }
                    },
                    negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                    onNegativeButtonClicked = onDismiss,
                    onDismiss = onDismiss,
                )

                inviteContactResult?.let { inviteResult ->
                    if (inviteResult.isSuccess) {
                        email?.let { email ->
                            inviteResult.getOrNull()?.let { result ->
                                SnackbarAttributes(
                                    message = result.getMessage(context, email),
                                    action = result.getAction(context),
                                    duration = SnackbarDuration.Long,
                                    actionClick = {
                                        result.getNavigation()?.let {
                                            navigateToContactRequests(it)
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        SnackbarAttributes(
                            message = stringResource(R.string.general_error),
                            duration = SnackbarDuration.Long,
                        )
                    }?.let {
                        coroutineScope.launch {
                            context.snackbarEventQueue.queueMessage(it)
                        }
                    }

                    onDismiss()
                }
            }
        }
    }
}