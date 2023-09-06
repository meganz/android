package mega.privacy.android.app.presentation.qrcode

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.qrcode.mapper.MyQRCodeTextErrorMapper
import mega.privacy.android.app.presentation.qrcode.mapper.SaveBitmapToFileMapper
import mega.privacy.android.app.presentation.qrcode.model.QRCodeUIState
import mega.privacy.android.app.presentation.qrcode.model.ScanResult
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.usecase.CopyToClipBoard
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.DeleteQRCodeUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.ResetContactLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for QRCodeActivity
 */
@HiltViewModel
class QRCodeViewModel @Inject constructor(
    private val copyToClipBoard: CopyToClipBoard,
    private val createContactLinkUseCase: CreateContactLinkUseCase,
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    private val deleteQRCodeUseCase: DeleteQRCodeUseCase,
    private val resetContactLinkUseCase: ResetContactLinkUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val saveBitmapToFile: SaveBitmapToFileMapper,
    private val queryScannedContactLinkUseCase: QueryScannedContactLinkUseCase,
    private val inviteContactUseCase: InviteContactUseCase,
    private val avatarContentMapper: AvatarContentMapper,
    private val myQRCodeTextErrorMapper: MyQRCodeTextErrorMapper,
    private val scannerHandler: ScannerHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QRCodeUIState())

    /**
     * UI state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Create QR code.
     */
    fun createQRCode() {
        Timber.d("create QR code")
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(myQRCodeState = MyCodeUIState.CreatingQRCode) }
                createContactLinkUseCase(renew = false)
            }.fold(
                onSuccess = { contactLink ->
                    val fullName = getUserFullNameUseCase(forceRefresh = true)
                    val avatarBgColor = getMyAvatarColorUseCase()
                    val localFile = getMyAvatarFileUseCase(true)

                    val avatarContent =
                        avatarContentMapper(
                            fullName = fullName,
                            localFile = localFile,
                            showBorder = true,
                            textSize = 38.sp,
                            backgroundColor = avatarBgColor,
                        )
                    _uiState.update {
                        it.copy(
                            myQRCodeState = MyCodeUIState.QRCodeAvailable(
                                contactLink = contactLink,
                                avatarBgColor = avatarBgColor,
                                avatarContent = avatarContent,
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update { it.copy(myQRCodeState = MyCodeUIState.Idle) }
                }
            )
        }
    }

    private suspend fun saveQRCodeToFile(bitmap: Bitmap) {
        getQRCodeFileUseCase()?.let { file ->
            saveBitmapToFile(bitmap, file)
        }
    }

    /**
     * Reset QR code
     */
    fun resetQRCode() {
        Timber.d("reset QR code")
        viewModelScope.launch {
            val qrCodeBackup = qrCodeBackup()
            runCatching {
                _uiState.update { it.copy(myQRCodeState = MyCodeUIState.CreatingQRCode) }
                resetContactLinkUseCase()
            }.fold(
                onSuccess = { contactLink ->
                    _uiState.update { it.copy(myQRCodeState = MyCodeUIState.QRCodeResetDone) }
                    val fullName = getUserFullNameUseCase(forceRefresh = true)
                    val avatarBgColor = getMyAvatarColorUseCase()
                    val avatarContent = avatarContentMapper(
                        fullName = fullName,
                        localFile = getMyAvatarFileUseCase(true),
                        showBorder = true,
                        textSize = 38.sp,
                        backgroundColor = getMyAvatarColorUseCase(),
                    )
                    _uiState.update {
                        it.copy(
                            myQRCodeState = MyCodeUIState.QRCodeAvailable(
                                contactLink = contactLink,
                                avatarBgColor = avatarBgColor,
                                avatarContent = avatarContent,
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(myQRCodeState = MyCodeUIState.Error(myQRCodeTextErrorMapper(error)))
                    }
                    qrCodeBackup?.let { _uiState.update { it.copy(myQRCodeState = qrCodeBackup) } }
                }
            )
        }
    }

    private fun qrCodeBackup(): MyCodeUIState.QRCodeAvailable? =
        (uiState.value.myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.copy()

    /**
     * Copy contact link to system clip board
     */
    fun copyContactLink() {
        Timber.d("copyLink")
        (uiState.value.myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.let {
            copyToClipBoard(label = "contact link", it.contactLink)
            setResultMessage(R.string.qrcode_link_copied)
        }
    }

    /**
     * Delete QR code
     */
    fun deleteQRCode() {
        Timber.d("deleteQRCode")
        val qrCodeBackup = qrCodeBackup()
        viewModelScope.launch {
            runCatching {
                (uiState.value.myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.let {
                    deleteQRCodeUseCase(contactLink = it.contactLink)
                }
            }.fold(
                onSuccess = {
                    _uiState.update { it.copy(myQRCodeState = MyCodeUIState.QRCodeDeleted) }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(myQRCodeState = MyCodeUIState.Error(myQRCodeTextErrorMapper(error)))
                    }
                    qrCodeBackup?.let { _uiState.update { it.copy(myQRCodeState = qrCodeBackup) } }
                }
            )
        }
    }

    /**
     * Start showing the share dialog.
     */
    fun startSharing() {
        viewModelScope.launch {
            runCatching { getQRCodeFileUseCase() }
                .onSuccess { file ->
                    file?.let { localFile ->
                        _uiState.update { it.copy(localQRCodeFile = localFile) }
                    }
                }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Reset the sharing state, indicating sharing is finished.
     */
    fun finishSharing() {
        _uiState.update {
            it.copy(
                localQRCodeFile = null
            )
        }
    }

    /**
     * Send invitation to new contact
     */
    fun sendInvite(contactHandle: Long, contactEmail: String) {
        viewModelScope.launch {
            runCatching { inviteContactUseCase(contactEmail, contactHandle, null) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            inviteContactResult = triggered(result),
                            scannedContactEmail = contactEmail
                        )
                    }
                }.onFailure { throwable ->
                    Timber.e(throwable)
                    _uiState.update { it.copy(inviteContactResult = triggered(InviteContactRequest.InvalidStatus)) }
                }
        }
    }

    /**
     * Query details of scanned contact and update the ui state
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     */
    fun queryContactLink(context: Context, scannedHandle: String) {
        viewModelScope.launch {
            runCatching { queryScannedContactLinkUseCase(scannedHandle) }
                .onSuccess { result ->
                    val scannedContactAvatar = avatarContentMapper(
                        fullName = result.contactName,
                        localFile = result.avatarFile,
                        backgroundColor = result.avatarColor
                            ?: context.getColor(R.color.red_600_red_300),
                        showBorder = false,
                        textSize = 36.sp
                    )
                    _uiState.update {
                        it.copy(
                            scannedContactLinkResult = triggered(result),
                            scannedContactAvatarContent = scannedContactAvatar,
                        )
                    }
                }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Start scanning of qr code
     */
    fun scanCode(context: Context) {
        viewModelScope.launch {
            runCatching { scannerHandler.scan() }
                .onSuccess {
                    when (it) {
                        is ScanResult.Success -> {
                            val contactLink = it.rawValue
                            contactLink?.let {
                                val s = contactLink.split("C!").toTypedArray()
                                if (s.size <= 1 || s[0] != Constants.SCANNED_CONTACT_BASE_URL) {
                                    setResultMessage(R.string.invalid_code)
                                } else {
                                    queryContactLink(context, s[1])
                                }
                            }
                        }

                        ScanResult.Cancel -> {}
                    }
                }
                .onFailure { error ->
                    Timber.e(error)
                    setResultMessage(R.string.general_text_error)
                }
        }
    }

    /**
     * Show result message of a operation
     *
     * @param messageId String ID of the message
     */
    private fun setResultMessage(messageId: Int) =
        _uiState.update { it.copy(resultMessage = triggered(messageId)) }

    /**
     * Reset and notify resultMessage is consumed
     */
    fun resetResultMessage() = _uiState.update { it.copy(resultMessage = consumed()) }

    /**
     * Reset and notify scannedContactLinkResult is consumed
     */
    fun resetScannedContactLinkResult() =
        _uiState.update { it.copy(scannedContactLinkResult = consumed()) }

    /**
     * Reset and notify inviteContactResult is consumed
     */
    fun resetInviteContactResult() = _uiState.update { it.copy(inviteContactResult = consumed()) }

    /**
     * Reset scannedContactEmail
     */
    fun resetScannedContactEmail() = _uiState.update { it.copy(scannedContactEmail = null) }

    /**
     * Reset scannedContactAvatarContent
     */
    fun resetScannedContactAvatar() =
        _uiState.update { it.copy(scannedContactAvatarContent = null) }
}