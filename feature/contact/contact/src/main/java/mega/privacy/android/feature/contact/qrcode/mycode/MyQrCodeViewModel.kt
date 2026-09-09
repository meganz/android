package mega.privacy.android.feature.contact.qrcode.mycode

import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.CopyToClipBoard
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.SaveFileToDestinationUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.DeleteQRCodeUseCase
import mega.privacy.android.domain.usecase.qrcode.ResetContactLinkUseCase
import mega.privacy.android.feature.contact.qrcode.mycode.mapper.MyQrCodeErrorMapper
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeContentState
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState
import mega.privacy.android.shared.account.presentation.mapper.AvatarContentMapper
import mega.privacy.android.shared.account.presentation.model.AvatarContent
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel driving the personal "My QR code" flow: creating, resetting and deleting the
 * current user's contact link, exposing their avatar, copying the link and saving the QR
 * image to the device.
 */
@HiltViewModel
class MyQrCodeViewModel @Inject constructor(
    private val copyToClipBoard: CopyToClipBoard,
    private val createContactLinkUseCase: CreateContactLinkUseCase,
    private val resetContactLinkUseCase: ResetContactLinkUseCase,
    private val deleteQRCodeUseCase: DeleteQRCodeUseCase,
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val saveFileToDestinationUseCase: SaveFileToDestinationUseCase,
    private val doesUriPathHaveSufficientSpaceUseCase: DoesUriPathHaveSufficientSpaceUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val avatarContentMapper: AvatarContentMapper,
    private val myQrCodeErrorMapper: MyQrCodeErrorMapper,
) : ViewModel() {

    private val contentState = MutableStateFlow<MyQrCodeContentState>(MyQrCodeContentState.Idle)
    private val messageChannel = Channel<StateEventWithContent<MyQrCodeMessage>>(Channel.BUFFERED)

    /**
     * UI state for the personal QR code screen.
     */
    val uiState: StateFlow<MyQrCodeUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            contentState,
            messageChannel.receiveAsFlow().onStart { emit(consumed()) },
        ) { content, message ->
            MyQrCodeUiState(
                contentState = content,
                message = message,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                MyQrCodeUiState(
                    contentState = MyQrCodeContentState.Idle,
                    message = consumed(),
                ),
            )
    }

    /**
     * Create a QR code for the current user's contact link.
     *
     * @param showLoader whether the creating state should request a loading indicator
     */
    fun createQRCode(showLoader: Boolean) {
        viewModelScope.launch {
            runCatching {
                contentState.update { MyQrCodeContentState.CreatingQRCode(showLoader) }
                createContactLinkUseCase(renew = false)
            }.onSuccess { contactLink ->
                runCatching { qrCodeAvailable(contactLink) }
                    .onSuccess { available -> contentState.update { available } }
                    .onFailure { error ->
                        Timber.e(error)
                        messageChannel.trySend(triggered(MyQrCodeMessage.GeneralError))
                        contentState.update { MyQrCodeContentState.Idle }
                    }
            }.onFailure { error ->
                Timber.e(error)
                messageChannel.trySend(triggered(MyQrCodeMessage.GeneralError))
                contentState.update { MyQrCodeContentState.Idle }
            }
        }
    }

    /**
     * Reset the QR code, generating a new contact link.
     */
    fun resetQRCode() {
        viewModelScope.launch {
            val backup = availableStateOrNull()
            runCatching {
                contentState.update { MyQrCodeContentState.CreatingQRCode(showLoader = true) }
                resetContactLinkUseCase()
            }.onSuccess { contactLink ->
                runCatching { qrCodeAvailable(contactLink) }
                    .onSuccess { available ->
                        contentState.update { available }
                        messageChannel.trySend(triggered(MyQrCodeMessage.ResetDone))
                    }
                    .onFailure { error -> emitOperationError(error, backup) }
            }.onFailure { error -> emitOperationError(error, backup) }
        }
    }

    /**
     * Delete the current QR code and its contact link.
     */
    fun deleteQRCode() {
        viewModelScope.launch {
            val backup = availableStateOrNull() ?: return@launch
            runCatching {
                deleteQRCodeUseCase(contactLink = backup.contactLink)
            }.onSuccess {
                contentState.update { MyQrCodeContentState.QRCodeDeleted }
            }.onFailure { error -> emitOperationError(error, backup) }
        }
    }

    private fun emitOperationError(
        error: Throwable,
        backup: MyQrCodeContentState.QRCodeAvailable?,
    ) {
        Timber.e(error)
        messageChannel.trySend(triggered(MyQrCodeMessage.OperationError(myQrCodeErrorMapper(error))))
        contentState.update { backup ?: MyQrCodeContentState.Idle }
    }

    /**
     * Copy the current contact link to the clipboard.
     */
    fun copyContactLink() {
        availableStateOrNull()?.let { available ->
            copyToClipBoard(label = CONTACT_LINK_CLIPBOARD_LABEL, text = available.contactLink)
            messageChannel.trySend(triggered(MyQrCodeMessage.ContactLinkCopied))
        }
    }

    /**
     * Save the QR code image to the given destination on the device.
     *
     * @param parentPath the destination directory to save the QR code image into
     */
    fun saveToFileSystem(parentPath: UriPath) {
        viewModelScope.launch {
            val qrFile = runCatching { getQRCodeFileUseCase() }
                .onFailure { Timber.e(it) }
                .getOrNull()

            if (qrFile == null) {
                messageChannel.trySend(triggered(MyQrCodeMessage.SaveError))
                return@launch
            }

            val hasSpace = runCatching {
                doesUriPathHaveSufficientSpaceUseCase(parentPath, qrFile.length())
            }.onFailure { Timber.e(it) }.getOrDefault(true)

            if (!hasSpace) {
                messageChannel.trySend(triggered(MyQrCodeMessage.NotEnoughSpace))
                return@launch
            }

            runCatching {
                val displayPath = runCatching {
                    getPathByDocumentContentUriUseCase(parentPath.value)
                }.getOrNull() ?: parentPath.value
                saveFileToDestinationUseCase(qrFile, parentPath)
                displayPath
            }.onSuccess { displayPath ->
                messageChannel.trySend(triggered(MyQrCodeMessage.SaveSuccess(displayPath)))
            }.onFailure { error ->
                Timber.e(error)
                messageChannel.trySend(triggered(MyQrCodeMessage.GeneralError))
            }
        }
    }

    /**
     * Notify that the current [MyQrCodeUiState.message] has been consumed.
     */
    fun onMessageConsumed() {
        messageChannel.trySend(consumed())
    }

    private fun availableStateOrNull(): MyQrCodeContentState.QRCodeAvailable? =
        contentState.value as? MyQrCodeContentState.QRCodeAvailable

    private suspend fun qrCodeAvailable(contactLink: String): MyQrCodeContentState.QRCodeAvailable {
        val avatarBgColor = getMyAvatarColorUseCase()
        return MyQrCodeContentState.QRCodeAvailable(
            contactLink = contactLink,
            avatarContent = buildAvatarContent(avatarBgColor),
            avatarBgColor = avatarBgColor,
            qrCodeFilePath = getQRCodeFileUseCase()?.absolutePath,
        )
    }

    private suspend fun buildAvatarContent(backgroundColor: Int): AvatarContent =
        avatarContentMapper(
            fullName = getUserFullNameUseCase(forceRefresh = true),
            localFile = runCatching { getMyAvatarFileUseCase(isForceRefresh = true) }.getOrNull(),
            showBorder = true,
            textSize = AVATAR_TEXT_SIZE.sp,
            backgroundColor = backgroundColor,
        )

    companion object {
        private const val CONTACT_LINK_CLIPBOARD_LABEL = "contact link"

        // Matches the legacy QRCode screen's avatar sizing.
        private const val AVATAR_TEXT_SIZE = 38f
    }
}
