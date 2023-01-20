package mega.privacy.android.app.presentation.qrcode.scan.model

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import java.io.File

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
 * @property finishActivityOnScanComplete Whether to finish activity on completing the scan
 * @property finishActivity Finish the current activity
 * @property avatarFile Avatar of the contact
 * @property avatarColor Color of the avatar
 */
data class ScanCodeState(
    @StringRes val dialogTitleContent: Int = -1,
    @StringRes val dialogTextContent: Int = -1,
    val contactNameContent: String? = null,
    val isContact: Boolean = false,
    val myEmail: String? = null,
    val handleContactLink: Long = -1,
    val success: Boolean = true,
    val printEmail: Boolean = false,
    val inviteDialogShown: Boolean = false,
    val inviteResultDialogShown: Boolean = false,
    val showInviteResultDialog: Boolean = false,
    val showInviteDialog: Boolean = false,
    val finishActivityOnScanComplete: Boolean = false,
    val finishActivity: Boolean = false,
    val avatarFile: File? = null,
    @ColorInt val avatarColor: Int? = null,
)