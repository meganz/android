package mega.privacy.android.app.presentation.qrcode

import android.graphics.Bitmap
import androidx.annotation.ColorInt
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
import mega.privacy.android.app.presentation.avatar.mapper.AvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.CombineQRCodeAndAvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.GetCircleBitmapMapper
import mega.privacy.android.app.presentation.qrcode.mapper.LoadBitmapFromFileMapper
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mapper.SaveBitmapToFileMapper
import mega.privacy.android.app.presentation.qrcode.model.QRCodeUIState
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
    private val avatarMapper: AvatarMapper,
    private val qrCodeMapper: QRCodeMapper,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val loadBitmapFromFile: LoadBitmapFromFileMapper,
    private val saveBitmapToFile: SaveBitmapToFileMapper,
    private val getCircleBitmap: GetCircleBitmapMapper,
    private val combineQRCodeAndAvatar: CombineQRCodeAndAvatarMapper,
    private val queryScannedContactLinkUseCase: QueryScannedContactLinkUseCase,
    private val inviteContactUseCase: InviteContactUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QRCodeUIState())

    /**
     * UI state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Create QR code
     *
     * @param width width of target bitmap
     * @param height height of target bitmap
     * @param penColor color of the QR code. This is ARGB format.
     * @param bgColor color of generated QR code. This is ARGB format.
     * @param avatarWidth width of avatar
     * @param avatarBorderWidth border width of avatar
     *
     */
    fun createQRCode(
        width: Int,
        height: Int,
        @ColorInt penColor: Int,
        @ColorInt bgColor: Int,
        avatarWidth: Int,
        avatarBorderWidth: Int,
        @ColorInt avatarBorderColor: Int,
    ) {
        viewModelScope.launch {
            runCatching {
                val contactLink = uiState.value.contactLink
                val qrCodeBitmap = uiState.value.qrCodeBitmap
                val hasLoaded = contactLink != null && qrCodeBitmap != null
                if (hasLoaded) {
                    _uiState.update {
                        it.copy(
                            qrCodeBitmap = qrCodeBitmap,
                            contactLink = contactLink,
                            hasQRCodeBeenDeleted = false
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(isInProgress = true)
                }

                loadQRCodeBitmapFromCache()?.let { bitmap ->
                    Timber.d("Cached QR code file loaded")
                    _uiState.update {
                        it.copy(qrCodeBitmap = bitmap)
                    }
                }

                val newContactLink = createContactLinkUseCase(false)
                generateQRCodeBitmap(
                    contactLink = newContactLink,
                    width = width,
                    height = height,
                    penColor = penColor,
                    bgColor = bgColor,
                    avatarWidth = avatarWidth,
                    avatarBorderWidth = avatarBorderWidth,
                    avatarBorderColor = avatarBorderColor,
                ).let { combinedBitmap ->
                    _uiState.update {
                        it.copy(
                            contactLink = newContactLink,
                            qrCodeBitmap = combinedBitmap,
                            hasQRCodeBeenDeleted = false
                        )
                    }
                }
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isInProgress = false)
                    }
                },
                onFailure = { error ->
                    Timber.e(error)
                    _uiState.update {
                        it.copy(isInProgress = false)
                    }
                }
            )
        }
    }

    private suspend fun loadQRCodeBitmapFromCache(): Bitmap? =
        getQRCodeFileUseCase()?.let { loadBitmapFromFile(it) }

    private suspend fun generateQRCodeBitmap(
        contactLink: String,
        width: Int,
        height: Int,
        @ColorInt penColor: Int,
        @ColorInt bgColor: Int,
        avatarWidth: Int,
        avatarBorderWidth: Int,
        @ColorInt avatarBorderColor: Int,
    ): Bitmap = combineQRCodeAndAvatar(
        qrCodeBitmap = qrCodeMapper(
            text = contactLink,
            width = width,
            height = height,
            penColor = penColor,
            bgColor = bgColor
        ),
        qrCodeWidth = width,
        qrCodeBgColor = bgColor,
        avatarBitmap = getAvatarBitmap(),
        avatarWidth = avatarWidth,
        avatarBorderWidth = avatarBorderWidth,
        avatarBorderColor = avatarBorderColor,
    ).also {
        saveQRCodeToFile(it)
    }

    private suspend fun getAvatarBitmap(): Bitmap {
        val userFullName = getUserFullNameUseCase(forceRefresh = false) ?: ""
        return getMyAvatarFileUseCase(isForceRefresh = false)
            ?.takeIf { it.exists() && it.length() > 0 }
            ?.let { avatarFile ->
                loadBitmapFromFile(avatarFile)
                    ?.let { getCircleBitmap(it) }
                    ?: avatarMapper.getDefaultAvatar(
                        color = getMyAvatarColorUseCase(),
                        text = userFullName,
                        isList = true
                    )
            } ?: avatarMapper.getDefaultAvatar(
            color = getMyAvatarColorUseCase(),
            text = userFullName,
            isList = true
        )
    }

    private suspend fun saveQRCodeToFile(bitmap: Bitmap) {
        getQRCodeFileUseCase()?.let { file ->
            saveBitmapToFile(bitmap, file)
        }
    }

    /**
     * Reset QR code
     *
     * @param width width of target bitmap
     * @param height height of target bitmap
     * @param penColor color of the QR code. This is ARGB format.
     * @param bgColor color of generated QR code. This is ARGB format.
     * @param avatarWidth width of avatar
     * @param avatarBorderWidth border width of avatar
     *
     */
    fun resetQRCode(
        width: Int,
        height: Int,
        @ColorInt penColor: Int,
        @ColorInt bgColor: Int,
        avatarWidth: Int,
        avatarBorderWidth: Int,
        @ColorInt avatarBorderColor: Int,
    ) {
        viewModelScope.launch {
            runCatching {
                resetContactLinkUseCase().let { contactLink ->
                    Timber.d("contact link created $contactLink")

                    generateQRCodeBitmap(
                        contactLink = contactLink,
                        width = width,
                        height = height,
                        penColor = penColor,
                        bgColor = bgColor,
                        avatarWidth = avatarWidth,
                        avatarBorderWidth = avatarBorderWidth,
                        avatarBorderColor = avatarBorderColor
                    ).let { qrCodeBitmap ->
                        _uiState.update {
                            it.copy(
                                contactLink = contactLink,
                                qrCodeBitmap = qrCodeBitmap,
                                hasQRCodeBeenDeleted = false,
                            )
                        }
                    }
                }
            }.fold(
                onSuccess = {
                    setResultMessage(R.string.qrcode_reset_successfully)
                },
                onFailure = { error ->
                    Timber.e(error)
                    setResultMessage(R.string.qrcode_reset_not_successfully)
                }

            )
        }
    }

    /**
     * Copy contact link to system clip board
     */
    fun copyContactLink() {
        Timber.d("copyLink")
        uiState.value.contactLink?.let { contactLink ->
            copyToClipBoard(label = "contact link", contactLink)
            setResultMessage(R.string.qrcode_link_copied)
        }
    }

    /**
     * Delete QR code
     */
    fun deleteQRCode() {
        Timber.d("deleteQRCode")
        viewModelScope.launch {
            runCatching {
                uiState.value.contactLink?.let { contactLink ->
                    deleteQRCodeUseCase(contactLink = contactLink)
                }
            }.fold(
                onSuccess = {
                    setResultMessage(R.string.qrcode_delete_successfully)
                    _uiState.update {
                        it.copy(
                            contactLink = null,
                            qrCodeBitmap = null,
                            hasQRCodeBeenDeleted = true
                        )
                    }
                },
                onFailure = {
                    Timber.e(it)
                    setResultMessage(R.string.qrcode_delete_not_successfully)
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
    fun sendInvite() {
        viewModelScope.launch {
            runCatching {
                inviteContactUseCase(
                    uiState.value.scannedContactLinkResult?.email ?: "",
                    uiState.value.scannedContactLinkResult?.handle ?: -1,
                    null
                )
            }.onSuccess { result ->
                _uiState.update { it.copy(inviteContactResult = result) }
            }.onFailure { throwable ->
                Timber.e(throwable)
                _uiState.update { it.copy(inviteContactResult = InviteContactRequest.InvalidStatus) }
            }
        }
    }

    /**
     * Query details of scanned contact and update the ui state
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     */
    fun queryContactLink(scannedHandle: String) {
        viewModelScope.launch {
            runCatching { queryScannedContactLinkUseCase(scannedHandle) }
                .onSuccess { result -> _uiState.update { it.copy(scannedContactLinkResult = result) } }
                .onFailure { Timber.e(it) }
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
    private fun resetResultMessage() = _uiState.update { it.copy(resultMessage = consumed()) }
}