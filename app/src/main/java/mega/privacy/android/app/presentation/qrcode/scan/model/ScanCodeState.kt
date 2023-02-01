package mega.privacy.android.app.presentation.qrcode.scan.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel]
 *
 * @property dialogTitleContent Id of string resource for dialog title
 * @property dialogTextContent Id of string resource fot dialog content
 * @property email Email
 * @property success Whether to clos the activity on dismissing the dialog
 * @property printEmail Whether to show email in the dialog content
 * @property inviteDialogShown Whether dialog is shown or not
 * @property inviteResultDialogShown Whether dialog is shown or not
 * @property showInviteResultDialog Whether to show dialog
 * @property showInviteDialog Whether to show dialog
 * @property finishActivityOnScanComplete Whether to finish activity on completing the scan
 * @property finishActivity Finish the current activity
 * @property scannedContactLinkResult Scanned contact details
 */
data class ScanCodeState(
    @StringRes val dialogTitleContent: Int = -1,
    @StringRes val dialogTextContent: Int = -1,
    val email: String? = null,
    val success: Boolean = true,
    val printEmail: Boolean = false,
    val inviteDialogShown: Boolean = false,
    val inviteResultDialogShown: Boolean = false,
    val showInviteResultDialog: Boolean = false,
    val showInviteDialog: Boolean = false,
    val finishActivityOnScanComplete: Boolean = false,
    val finishActivity: Boolean = false,
    val scannedContactLinkResult: ScannedContactLinkResult? = null,
)