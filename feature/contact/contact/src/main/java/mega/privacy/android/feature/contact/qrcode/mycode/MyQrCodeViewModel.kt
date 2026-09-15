package mega.privacy.android.feature.contact.qrcode.mycode

import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
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
import mega.privacy.android.feature.contact.qrcode.mycode.MyQrCodeViewModel.Companion.MINIMUM_LOADING_DURATION
import mega.privacy.android.feature.contact.qrcode.mycode.mapper.MyQrCodeErrorMapper
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState
import mega.privacy.android.feature.contact.qrcode.mycode.model.QrCode
import mega.privacy.android.shared.account.presentation.mapper.AvatarContentMapper
import mega.privacy.android.shared.account.presentation.model.AvatarContent
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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

    private val qrCodeChannel = Channel<QrData>()

    /**
     * Mirrors the latest value sent on [qrCodeChannel]. The channel is a conduit to the UI
     * collector, not a state holder, so restoring after a failed reset needs its own record.
     */
    private var lastQrData: QrData = QrData.Loading
    private val messageChannel = Channel<StateEventWithContent<MyQrCodeMessage>>(Channel.BUFFERED)

    /**
     * UI state for the personal QR code screen.
     */
    val uiState: StateFlow<MyQrCodeUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            qrCodeChannel.receiveAsFlow().onStart { createQRCode() },
            messageChannel.receiveAsFlow().onStart { emit(consumed()) },
        ) { qrData, message ->
            when (qrData) {
                is QrData.Data -> MyQrCodeUiState.QRCodeAvailable(qrData.qrCode, message)
                QrData.Empty -> MyQrCodeUiState.QRCodeUnavailable(message)
                QrData.Loading -> MyQrCodeUiState.CreatingQRCode
            }
        }.asUiStateFlow(
            viewModelScope,
            MyQrCodeUiState.CreatingQRCode,
        )
    }

    sealed interface QrData {
        data object Loading : QrData
        data object Empty : QrData
        data class Data(val qrCode: QrCode) : QrData
    }

    /**
     * Creates a new qr code
     *
     */
    fun createQRCode() {
        viewModelScope.launch {
            emitQrData(QrData.Loading)
            withMinimumLoadingDuration {
                runCatching { getQrCodeData(createContactLinkUseCase(renew = false)) }
            }.onSuccess {
                emitQrData(QrData.Data(it))
            }.onFailure {
                Timber.e(it)
                emitQrData(QrData.Empty)
                messageChannel.send(triggered(MyQrCodeMessage.GeneralError))
            }
        }
    }

    /**
     * Reset the QR code, generating a new contact link.
     */
    fun resetQRCode() {
        viewModelScope.launch {
            val current = lastQrData
            emitQrData(QrData.Loading)
            withMinimumLoadingDuration {
                runCatching { getQrCodeData(resetContactLinkUseCase()) }
            }.onSuccess {
                emitQrData(QrData.Data(it))
                messageChannel.send(triggered(MyQrCodeMessage.ResetDone))
            }.onFailure {
                Timber.e(it)
                emitQrData(current)
                messageChannel.send(
                    triggered(MyQrCodeMessage.OperationError(myQrCodeErrorMapper(it)))
                )
            }
        }
    }


    fun deleteQRCode(contactLink: String) {
        viewModelScope.launch {
            runCatching {
                deleteQRCodeUseCase(contactLink = contactLink)
            }.onSuccess {
                emitQrData(QrData.Empty)
            }.onFailure {
                Timber.e(it)
                messageChannel.send(
                    triggered(MyQrCodeMessage.OperationError(myQrCodeErrorMapper(it)))
                )
            }
        }
    }



    /**
     * Runs [block], holding the caller for at least [MINIMUM_LOADING_DURATION] so that a loading
     * state already on screen stays readable — a spinner that appears and vanishes within a frame
     * reads as a glitch. Slower work is not delayed further. Failures are covered too, so a fast
     * error does not flash the spinner either.
     */
    private suspend fun <T> withMinimumLoadingDuration(block: suspend () -> T): T = coroutineScope {
        val minimumElapsed = async { delay(MINIMUM_LOADING_DURATION) }
        val result = block()
        minimumElapsed.await()
        result
    }

    private suspend fun emitQrData(qrData: QrData) {
        lastQrData = qrData
        qrCodeChannel.send(qrData)
    }

    /**
     * Copy the current contact link to the clipboard.
     */
    fun copyContactLink(contactLink: String) {
        copyToClipBoard(label = CONTACT_LINK_CLIPBOARD_LABEL, text = contactLink)
        messageChannel.trySend(triggered(MyQrCodeMessage.ContactLinkCopied))
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

    private suspend fun getQrCodeData(contactLink: String): QrCode {
        val avatarBgColor = getMyAvatarColorUseCase()
        return QrCode(
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

        // Provisional floor for how long the generating state stays on screen — confirm with design.
        private val MINIMUM_LOADING_DURATION = 600.milliseconds
    }
}
