package mega.privacy.android.app.presentation.qrcode.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.dialogContent
import mega.privacy.android.app.presentation.extensions.dialogTitle
import mega.privacy.android.app.presentation.extensions.printEmail
import mega.privacy.android.app.presentation.qrcode.scan.model.ScanCodeState
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLink
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for [ScanCodeFragment]
 */
@HiltViewModel
class ScanCodeViewModel @Inject constructor(
    private val queryScannedContactLink: QueryScannedContactLink,
    private val inviteContactUseCase: InviteContactUseCase,
) : ViewModel() {

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
        _state.update { it.copy(email = email) }
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
     * Update boolean to finish the activity on completing the scan of QR code
     */
    fun updateFinishActivityOnScanComplete(value: Boolean) {
        _state.update { it.copy(finishActivityOnScanComplete = value) }
    }

    /**
     * Update state to finish the activity
     */
    private fun finishActivity() {
        _state.update { it.copy(finishActivity = true) }
    }

    /**
     * Query details of scanned contact and update the ui state
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     */
    fun queryContactLink(scannedHandle: String) {
        viewModelScope.launch {
            with(queryScannedContactLink(scannedHandle)) {
                updateMyEmail(email)

                if (state.value.finishActivityOnScanComplete) {
                    finishActivity()
                    return@launch
                }

                when (qrCodeQueryResult) {
                    QRCodeQueryResults.CONTACT_QUERY_OK -> {
                        Timber.d("Contact link query ${handle}_${email}_${contactName}_${avatarFile}")
                        showInviteDialog(this)
                    }

                    QRCodeQueryResults.CONTACT_QUERY_EEXIST -> {
                        showInviteResultDialog(
                            qrCodeQueryResult.dialogTitle,
                            qrCodeQueryResult.dialogContent,
                            success = true,
                            printEmail = true
                        )
                    }

                    else -> {
                        showInviteResultDialog(
                            qrCodeQueryResult.dialogTitle,
                            qrCodeQueryResult.dialogContent,
                            success = false,
                            printEmail = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Send invitation to new contact
     */
    fun sendInvite() {
        viewModelScope.launch {
            runCatching {
                inviteContactUseCase(
                    state.value.email ?: "",
                    state.value.scannedContactLinkResult?.handle ?: -1,
                    null
                )
            }.onSuccess { request ->
                showInviteResultDialog(
                    title = request.dialogTitle,
                    text = request.dialogContent,
                    success = true,
                    printEmail = request.printEmail
                )
            }.onFailure {
                Timber.e(it)
                showInviteResultDialog(
                    title = R.string.invite_not_sent,
                    text = R.string.invite_not_sent_text_error,
                    success = false,
                    printEmail = false
                )
            }
        }
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
     * @param scannedContactLinkResult Scanned contact details
     */
    fun showInviteDialog(scannedContactLinkResult: ScannedContactLinkResult) {
        _state.update {
            it.copy(
                scannedContactLinkResult = scannedContactLinkResult,
                showInviteResultDialog = false,
                showInviteDialog = true
            )
        }
    }
}