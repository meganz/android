package mega.privacy.android.feature.contact.qrcode.scan.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.android.core.ui.components.dialogs.BasicContactDialog
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.feature.contact.qrcode.scan.model.ScanQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.scan.model.ScanQrCodeUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Stateless scan / invite surface. It has no chrome of its own: it overlays the result dialogs for
 * the QR scan flow driven by [mega.privacy.android.feature.contact.qrcode.scan.ScanQrCodeViewModel]
 * — the scanned-contact invite dialog, the invite-result dialog, and error dialogs for
 * [ScanQrCodeMessage]. The scan itself is launched by the host through the view model.
 *
 * @param uiState
 * @param onViewContact invoked with the email of a scanned contact who is already a contact.
 * @param onInviteContact invoked with the handle and email of a scanned contact to invite.
 * @param onScannedContactLinkResultConsumed invoked when the scanned-contact dialog is dismissed.
 * @param onInviteContactResultConsumed invoked when the invite-result dialog is dismissed.
 * @param onScanCancelConsumed invoked once a scan cancellation is handled.
 * @param onMessageConsumed invoked when the error dialog is dismissed.
 * @param onScannedContactEmailReset invoked when the invite-result dialog is dismissed.
 * @param onScannedContactAvatarReset invoked when the invite dialog is dismissed.
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanQrCodeScreen(
    uiState: ScanQrCodeUiState,
    onViewContact: (String) -> Unit,
    onInviteContact: (Long, String) -> Unit,
    onScannedContactLinkResultConsumed: () -> Unit,
    onInviteContactResultConsumed: () -> Unit,
    onScanCancelConsumed: () -> Unit,
    onMessageConsumed: () -> Unit,
    onScannedContactEmailReset: () -> Unit,
    onScannedContactAvatarReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EventEffect(
        event = uiState.scanCancel,
        onConsumed = onScanCancelConsumed,
    ) {}

    val scannedContact = uiState.scannedContactLinkResult
    if (scannedContact is StateEventWithContentTriggered) {
        val result = scannedContact.content
        ScannedContactDialog(
            modifier = modifier,
            result = result,
            avatarContentColor = result.avatarColor?.let { Color(it) } ?: Color.Unspecified,
            onViewContact = onViewContact,
            onInviteContact = onInviteContact,
            onDismiss = {
                onScannedContactLinkResultConsumed()
                onScannedContactAvatarReset()
            },
        )
    }

    val inviteResult = uiState.inviteContactResult
    if (inviteResult is StateEventWithContentTriggered) {
        InviteResultDialog(
            request = inviteResult.content,
            email = uiState.scannedContactEmail,
            onDismiss = {
                onInviteContactResultConsumed()
                onScannedContactEmailReset()
            },
        )
    }

    val message = uiState.message
    if (message is StateEventWithContentTriggered) {
        BasicDialog(
            title = stringResource(sharedR.string.contacts_qr_invitation_not_sent_dialog_title),
            description = stringResource(
                when (message.content) {
                    ScanQrCodeMessage.InvalidCode -> sharedR.string.contacts_qr_invalid_code_dialog_body
                    ScanQrCodeMessage.GeneralError -> sharedR.string.general_text_error
                },
            ),
            positiveButtonText = stringResource(sharedR.string.general_ok),
            onPositiveButtonClicked = onMessageConsumed,
            onDismiss = onMessageConsumed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannedContactDialog(
    result: ScannedContactLinkResult,
    avatarContentColor: Color,
    onViewContact: (String) -> Unit,
    onInviteContact: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (result.qrCodeQueryResult) {
        QRCodeQueryResults.CONTACT_QUERY_OK -> BasicContactDialog(
            modifier = modifier,
            contactName = result.contactName,
            contactEmail = result.email,
            contactAvatarFile = result.avatarFile,
            contactAvatarColor = avatarContentColor,
            description = if (result.isContact) {
                stringResource(sharedR.string.contacts_qr_contact_already_exists, result.contactName)
            } else {
                null
            },
            positiveButtonText = if (result.isContact) {
                stringResource(sharedR.string.contacts_qr_view_contact_action)
            } else {
                stringResource(sharedR.string.contact_invite)
            },
            onPositiveButtonClicked = {
                if (result.isContact) {
                    onViewContact(result.email)
                } else {
                    onInviteContact(result.handle, result.email)
                }
                onDismiss()
            },
            negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
            onNegativeButtonClicked = onDismiss,
            onDismiss = onDismiss,
        )

        QRCodeQueryResults.CONTACT_QUERY_EEXIST -> BasicDialog(
            modifier = modifier,
            title = stringResource(sharedR.string.contacts_qr_invitation_not_sent_dialog_title),
            description = stringResource(
                sharedR.string.contacts_qr_already_contact_dialog_body,
                result.email,
            ),
            positiveButtonText = stringResource(sharedR.string.general_ok),
            onPositiveButtonClicked = onDismiss,
            onDismiss = onDismiss,
        )

        QRCodeQueryResults.CONTACT_QUERY_DEFAULT -> BasicDialog(
            modifier = modifier,
            title = stringResource(sharedR.string.contacts_qr_invitation_not_sent_dialog_title),
            description = stringResource(sharedR.string.contacts_qr_invalid_code_dialog_body),
            positiveButtonText = stringResource(sharedR.string.general_ok),
            onPositiveButtonClicked = onDismiss,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Result dialog shown after a contact invitation attempt. The title and body reflect the
 * [request] outcome; [email] fills the placeholder in outcomes that name the invited address.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InviteResultDialog(
    request: InviteContactRequest,
    email: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier,
        title = stringResource(request.dialogTitleRes()),
        description = request.dialogBody(email),
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

private fun InviteContactRequest.dialogTitleRes(): Int = when (this) {
    InviteContactRequest.Sent -> sharedR.string.contacts_qr_invite_sent_dialog_title
    else -> sharedR.string.contacts_qr_invitation_not_sent_dialog_title
}

@Composable
private fun InviteContactRequest.dialogBody(email: String?): String = when (this) {
    InviteContactRequest.Sent ->
        stringResource(sharedR.string.contacts_qr_invite_sent_dialog_body, email.orEmpty())

    InviteContactRequest.AlreadyContact ->
        stringResource(sharedR.string.contacts_qr_contact_already_exists, email.orEmpty())

    InviteContactRequest.AlreadySent ->
        stringResource(sharedR.string.contacts_qr_already_sent_dialog_body, email.orEmpty())

    InviteContactRequest.InvalidEmail ->
        stringResource(sharedR.string.contacts_qr_own_email_dialog_body)

    else -> stringResource(sharedR.string.contacts_qr_invite_error_dialog_body)
}

@CombinedThemePreviews
@Composable
private fun ScanQrCodeInviteDialogPreview() {
    AndroidThemeForPreviews {
        ScanQrCodeScreen(
            uiState = ScanQrCodeUiState(
                scannedContactLinkResult = triggered(
                    ScannedContactLinkResult(
                        contactName = "Alice Anderson",
                        email = "alice@example.com",
                        handle = 1L,
                        isContact = false,
                        qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK,
                        avatarColor = 0xFF2E7D32.toInt(),
                    ),
                ),
                scannedContactAvatarContent = null,
                scannedContactEmail = null,
                inviteContactResult = consumed(),
                scanCancel = consumed,
                message = consumed(),
            ),
            onViewContact = {},
            onInviteContact = { _, _ -> },
            onScannedContactLinkResultConsumed = {},
            onInviteContactResultConsumed = {},
            onScanCancelConsumed = {},
            onMessageConsumed = {},
            onScannedContactEmailReset = {},
            onScannedContactAvatarReset = {},
        )
    }
}
