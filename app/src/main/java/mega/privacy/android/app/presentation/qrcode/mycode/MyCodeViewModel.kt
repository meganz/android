package mega.privacy.android.app.presentation.qrcode.mycode

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyQRCodeUIState
import mega.privacy.android.domain.usecase.CopyToClipBoard
import mega.privacy.android.domain.usecase.CreateContactLink
import mega.privacy.android.domain.usecase.DeleteQRCode
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetMyAvatarColor
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.GetQRCodeFile
import mega.privacy.android.domain.usecase.ResetContactLink
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [MyCodeFragment]
 */
@HiltViewModel
class MyCodeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val copyToClipBoard: CopyToClipBoard,
    private val createContactLink: CreateContactLink,
    private val getQRCodeFile: GetQRCodeFile,
    private val deleteQRCode: DeleteQRCode,
    private val resetContactLink: ResetContactLink,
    private val avatarMapper: AvatarMapper,
    private val qrCodeMapper: QRCodeMapper,
    private val getMyAvatarColor: GetMyAvatarColor,
    private val getCurrentUserFullName: GetCurrentUserFullName,
    private val getMyAvatarFile: GetMyAvatarFile,
    private val loadBitmapFromFile: LoadBitmapFromFileMapper,
    private val saveBitmapToFile: SaveBitmapToFileMapper,
    private val getCircleBitmap: GetCircleBitmapMapper,
    private val combineQRCodeAndAvatar: CombineQRCodeAndAvatarMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyQRCodeUIState())

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

                loadQRCodeBitmapFromCache()?.let { bitmap ->
                    Timber.d("Cached QR code file loaded")
                    _uiState.update {
                        it.copy(qrCodeBitmap = bitmap)
                    }
                }

                _uiState.update {
                    it.copy(isInProgress = true)
                }

                createContactLink(renew = false)?.let { newContactLink ->
                    Timber.d("contact link created $newContactLink")
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

    private suspend fun loadQRCodeBitmapFromCache(): Bitmap? =
        getQRCodeFile()
            ?.takeIf { it.exists() }
            ?.let { loadBitmapFromFile(it) }

    private suspend fun getAvatarBitmap(): Bitmap {

        val userFullName = getCurrentUserFullName(
            forceRefresh = false,
            defaultFirstName = context.getString(R.string.first_name_text),
            defaultLastName = context.getString(R.string.lastname_text)
        )

        return getMyAvatarFile(isForceRefresh = false)?.takeIf { it.exists() && it.length() > 0 }
            ?.let { avatarFile ->
                loadBitmapFromFile(avatarFile)
                    ?.let { getCircleBitmap(it) }
                    ?: avatarMapper.getDefaultAvatar(
                        color = getMyAvatarColor(),
                        text = userFullName,
                        isList = true
                    )
            } ?: avatarMapper.getDefaultAvatar(
            color = getMyAvatarColor(),
            text = userFullName,
            isList = true
        )
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
                resetContactLink().let { contactLink ->
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
                    setSnackBarMessage(R.string.qrcode_reset_successfully)
                },
                onFailure = { error ->
                    Timber.e(error)
                    setSnackBarMessage(R.string.qrcode_reset_not_successfully)
                }

            )
        }
    }

    /**
     * Show snack bar with message.
     *
     * @param stringResId String ID in the snack bar. null to disable snack bar.
     */
    fun setSnackBarMessage(stringResId: Int?) {
        _uiState.update {
            it.copy(
                snackBarMessage = stringResId
            )
        }
    }

    private suspend fun saveQRCodeToFile(bitmap: Bitmap) {
        getQRCodeFile()?.let { file ->
            saveBitmapToFile(bitmap, file)
        }
    }

    /**
     * Copy contact link to system clip board
     */
    fun copyContactLink() {
        Timber.d("copyLink")
        uiState.value.contactLink?.let { contactLink ->
            copyToClipBoard(label = "contact link", contactLink)
            _uiState.update {
                it.copy(snackBarMessage = R.string.qrcode_link_copied)
            }
        }
    }

    /**
     * Delete QR code
     */
    fun deleteQR() {
        Timber.d("deleteQRCode")
        viewModelScope.launch {
            runCatching {
                uiState.value.contactLink?.let { contactLink ->
                    deleteQRCode(contactLink = contactLink)
                }
            }.fold(
                onSuccess = {
                    setSnackBarMessage(R.string.qrcode_delete_successfully)
                    _uiState.update {
                        it.copy(
                            contactLink = null,
                            qrCodeBitmap = null,
                            hasQRCodeBeenDeleted = true
                        )
                    }
                },
                onFailure = {
                    Timber.w(it)
                    setSnackBarMessage(R.string.qrcode_delete_not_successfully)
                }
            )
        }
    }

    /**
     * Start showing the share dialog.
     */
    fun startSharing() {
        viewModelScope.launch {
            runCatching {
                getQRCodeFile()
                    ?.takeIf { it.exists() }
                    ?.let { localFile ->
                        _uiState.update {
                            it.copy(
                                localQRCodeFile = localFile
                            )
                        }
                    }
            }.onFailure {
                Timber.e(it)
            }
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
}