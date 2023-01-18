package mega.privacy.android.app.presentation.qrcode.scan.model

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel]
 *
 * @property dialogTitleContent Id of string resource for dialog title
 * @property dialogTextContent Id of string resource fot dialog content
 * @property contactNameContent Name of the scanned qr code
 * @property isContact Whether scanned code is of existing contact
 * @property myEmail Email
 * @property handleContactLink Handle of the scanned qr code
 * @property success Whether to clos the activity on dismissing the dialog
 * @property printEmail Whether to show email in the dialog content
 * @property inviteDialogShown Whether dialog is shown or not
 * @property inviteResultDialogShown Whether dialog is shown or not
 * @property showInviteResultDialog Whether to show dialog
 * @property showInviteDialog Whether to show dialog
 */
data class ScanCodeState(
    var dialogTitleContent: Int = -1,
    var dialogTextContent: Int = -1,
    var contactNameContent: String? = null,
    var isContact: Boolean = false,
    var myEmail: String? = null,
    var handleContactLink: Long = -1,
    var success: Boolean = true,
    var printEmail: Boolean = false,
    var inviteDialogShown: Boolean = false,
    var inviteResultDialogShown: Boolean = false,
    var showInviteResultDialog: Boolean = false,
    var showInviteDialog: Boolean = false
)