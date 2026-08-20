package mega.privacy.android.app.presentation.contact.invite.navigation

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.utils.Constants
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.feature.contact.invite.model.InvitationResult
import mega.privacy.android.feature.contact.invite.model.InviteMessage
import mega.privacy.android.feature.contact.invite.model.SmsInvite
import mega.privacy.android.feature.contact.invite.navigation.InviteContactEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * App-module host for the invite-contact flow. Supplies the platform-only side-effects the feature
 * module cannot own — launching the SMS composer, the share-link chooser and the personal QR-code
 * screen, and surfacing the invitation-result snackbar (whose optional action navigates to the
 * [ContactRequestsNavKey] contact-request destination) — then delegates rendering to [InviteContactEntry].
 *
 * @param navigationHandler
 * @param isFromAchievement whether the flow was launched from the achievements screen.
 */
@Composable
fun InviteContactAppHost(
    navigationHandler: NavigationHandler,
    isFromAchievement: Boolean,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()

    InviteContactEntry(
        navigationHandler = navigationHandler,
        isFromAchievement = isFromAchievement,
        onShareLink = { contactLink ->
            val message = context.getString(
                R.string.invite_contacts_to_start_chat_text_message,
                contactLink,
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, message)
                type = Constants.TYPE_TEXT_PLAIN
            }
            context.startActivity(
                Intent.createChooser(
                    sendIntent,
                    context.getString(R.string.invite_contact_chooser_title),
                )
            )
        },
        onOpenMyQr = {
            context.startActivity(Intent(context, QRCodeComposeActivity::class.java))
        },
        onSendSms = { smsInvite ->
            val recipient = "smsto:${smsInvite.phoneNumbers.joinToString(separator = ";")}"
            val smsBody = context.getString(
                R.string.invite_contacts_to_start_chat_text_message,
                smsInvite.contactLink,
            )
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(recipient)).apply {
                putExtra("sms_body", smsBody)
            }
            context.startActivity(smsIntent)
        },
        onShowInviteSnackbar = { result ->
            val host = snackbarHostState ?: return@InviteContactEntry
            val message = result.messages.joinToString(separator = "") { it.resolve(context.resources) }
            val actionLabel = result.actionLabelId?.let(context::getString)
            coroutineScope.launch {
                if (actionLabel == null) {
                    host.showAutoDurationSnackbar(message)
                } else {
                    val snackbarResult = host.showSnackbar(message, actionLabel)
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        val navType = when (result.actionLabelId) {
                            R.string.tab_received_requests -> ContactRequestsNavKey.NavType.ReceivedRequests
                            else -> ContactRequestsNavKey.NavType.SentRequests
                        }
                        navigationHandler.navigate(ContactRequestsNavKey(navType))
                    }
                }
            }
        },
    )
}

private fun InviteMessage.resolve(resources: Resources): String = when (this) {
    is InviteMessage.Sent -> resources.getQuantityString(
        R.plurals.contact_snackbar_invite_contact_requests_sent,
        count,
        count,
    )

    is InviteMessage.NotSent -> resources.getQuantityString(
        R.plurals.contact_snackbar_invite_contact_requests_not_sent,
        count,
        count,
    )

    is InviteMessage.AlreadyRequested -> if (count == 1) {
        resources.getString(sharedR.string.contacts_invite_already_received)
    } else {
        resources.getString(sharedR.string.contacts_invites_already_received)
    }
}
