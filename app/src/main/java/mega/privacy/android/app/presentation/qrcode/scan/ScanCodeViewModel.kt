package mega.privacy.android.app.presentation.qrcode.scan

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.qrcode.scan.model.ScanCodeState
import javax.inject.Inject

/**
 * View model for [ScanCodeFragment]
 */
@HiltViewModel
class ScanCodeViewModel @Inject constructor() : ViewModel() {

    /**
     * The ScanCode UI State
     */
    private val _state = MutableStateFlow(ScanCodeState())

    /**
     * The ScanCode UI State accessible outside the ViewModel
     */
    val state: StateFlow<ScanCodeState> = _state

    /**
     * Method to update the email
     *
     * @param email The email to update
     */
    fun updateMyEmail(email: String) {
        _state.update { it.copy(myEmail = email) }
    }

    /**
     * Updates the boolean which determines dialog was shown or not during config changes
     *
     * @param value Value indicating dialog is shown or not
     */
    fun updateInviteShown(value: Boolean) {
        _state.update { it.copy(inviteDialogShown = value) }
    }

    /**
     * Updates the boolean which determines dialog was shown or not during config changes
     *
     * @param value Value indicating dialog is shown or not
     */
    fun updateInviteResultDialogShown(value: Boolean) {
        _state.update { it.copy(inviteResultDialogShown = value) }
    }

    /**
     * Updates the boolean which determines whether to show dialog or not
     *
     * @param value Value indicating dialog should be shown or not
     */
    fun updateShowInviteResultDialog(value: Boolean) {
        _state.update { it.copy(showInviteResultDialog = value) }
    }

    /**
     * Updates the boolean which determines whether to show dialog or not
     *
     * @param value Value indicating dialog should be shown or not
     */
    fun updateShowInviteDialog(value: Boolean) {
        _state.update { it.copy(showInviteDialog = value) }
    }

    /**
     * Method to update the Ui State to show the dialog with the result of the invite operation
     *
     * @param title Title of the dialog
     * @param text Content of the dialog
     * @param success Whether to close the activity on dialog dismiss
     * @param printEmail Whether to show email or not in the content
     */
    fun showInviteResultDialog(title: Int, text: Int, success: Boolean, printEmail: Boolean) {
        _state.update {
            it.copy(
                dialogTitleContent = title,
                dialogTextContent = text,
                success = success,
                printEmail = printEmail,
                showInviteResultDialog = true,
                showInviteDialog = false
            )
        }
    }

    /**
     * Method to update the Ui State to show dialog after scanning of qr code
     *
     * @param contactName Name of the scanned contact
     * @param myEmail Email
     * @param isContact Whether scanned code is already a contact or not
     * @param handle Handle of the scanned qr code
     */
    fun showInviteDialog(contactName: String, myEmail: String, isContact: Boolean, handle: Long) {
        _state.update {
            it.copy(
                contactNameContent = contactName,
                myEmail = myEmail,
                isContact = isContact,
                handleContactLink = handle,
                showInviteResultDialog = false,
                showInviteDialog = true
            )
        }
    }
}