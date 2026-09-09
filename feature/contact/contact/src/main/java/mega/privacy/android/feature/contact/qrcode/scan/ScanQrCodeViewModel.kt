package mega.privacy.android.feature.contact.qrcode.scan

import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.MlKitException
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
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
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanResult
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanner
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScannerModuleIsNotInstalled
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.qrcode.ParseScannedContactLinkHandleUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.feature.contact.qrcode.scan.model.ScanQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.scan.model.ScanQrCodeUiState
import mega.privacy.android.shared.account.presentation.mapper.AvatarContentMapper
import mega.privacy.android.shared.account.presentation.model.AvatarContent
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel driving the QR code scan / invite flow: scanning a barcode, parsing and querying the
 * scanned contact link, resolving the scanned contact's avatar and sending a contact invitation.
 */
@HiltViewModel
class ScanQrCodeViewModel @Inject constructor(
    private val scannerHandler: BarcodeScanner,
    private val parseScannedContactLinkHandleUseCase: ParseScannedContactLinkHandleUseCase,
    private val queryScannedContactLinkUseCase: QueryScannedContactLinkUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val avatarContentMapper: AvatarContentMapper,
) : ViewModel() {

    private val scannedContactAvatar = MutableStateFlow<AvatarContent?>(null)
    private val scannedContactEmail = MutableStateFlow<String?>(null)
    private val scannedContactLinkResultChannel =
        Channel<StateEventWithContent<ScannedContactLinkResult>>(Channel.BUFFERED)
    private val inviteContactResultChannel =
        Channel<StateEventWithContent<InviteContactRequest>>(Channel.BUFFERED)
    private val scanCancelChannel = Channel<StateEvent>(Channel.BUFFERED)
    private val messageChannel = Channel<StateEventWithContent<ScanQrCodeMessage>>(Channel.BUFFERED)

    /**
     * UI state for the scan / invite screen.
     */
    val uiState: StateFlow<ScanQrCodeUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            scannedContactLinkResultChannel.receiveAsFlow().onStart { emit(consumed()) },
            scannedContactAvatar,
            scannedContactEmail,
            inviteContactResultChannel.receiveAsFlow().onStart { emit(consumed()) },
            combine(
                scanCancelChannel.receiveAsFlow().onStart { emit(consumed) },
                messageChannel.receiveAsFlow().onStart { emit(consumed()) },
            ) { scanCancel, message -> scanCancel to message },
        ) { scannedResult, avatar, email, inviteResult, (scanCancel, message) ->
            ScanQrCodeUiState(
                scannedContactLinkResult = scannedResult,
                scannedContactAvatarContent = avatar,
                scannedContactEmail = email,
                inviteContactResult = inviteResult,
                scanCancel = scanCancel,
                message = message,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                ScanQrCodeUiState(
                    scannedContactLinkResult = consumed(),
                    scannedContactAvatarContent = null,
                    scannedContactEmail = null,
                    inviteContactResult = consumed(),
                    scanCancel = consumed,
                    message = consumed(),
                ),
            )
    }

    /**
     * Start scanning a QR code and process the result.
     */
    fun scanCode() {
        viewModelScope.launch {
            runCatching { scannerHandler.scanBarcode() }
                .onSuccess { scanResult ->
                    when (scanResult) {
                        is BarcodeScanResult.Success -> scanResult.rawValue?.let { contactLink ->
                            parseScannedContactLinkHandleUseCase(contactLink)
                                ?.let { handle -> queryScannedContact(handle) }
                                ?: messageChannel.trySend(triggered(ScanQrCodeMessage.InvalidCode))
                        } ?: messageChannel.trySend(triggered(ScanQrCodeMessage.InvalidCode))

                        BarcodeScanResult.Cancelled -> scanCancelChannel.trySend(triggered)
                    }
                }
                .onFailure { error ->
                    Timber.e(error)
                    when {
                        error is BarcodeScannerModuleIsNotInstalled -> Unit
                        // On some devices (e.g. Pixel running Android 14+) pressing back in the GMS
                        // barcode scanner fires onFailure with MlKitException.INTERNAL instead of
                        // onCanceled. Treat it as a silent cancellation to avoid a misleading error.
                        // See: https://issuetracker.google.com/issues/261579118
                        error is MlKitException && error.errorCode == MlKitException.INTERNAL -> Unit
                        else -> messageChannel.trySend(triggered(ScanQrCodeMessage.GeneralError))
                    }
                }
        }
    }

    /**
     * Query the details of a scanned contact and update the state.
     *
     * @param scannedHandle base 64 handle of the scanned QR code
     */
    fun queryContactLink(scannedHandle: String) {
        viewModelScope.launch { queryScannedContact(scannedHandle) }
    }

    /**
     * Send an invitation to a scanned contact.
     *
     * @param contactHandle handle of the contact to invite
     * @param contactEmail email of the contact to invite
     */
    fun sendInvite(contactHandle: Long, contactEmail: String) {
        viewModelScope.launch {
            runCatching { inviteContactWithHandleUseCase(contactEmail, contactHandle, null) }
                .onSuccess { result ->
                    scannedContactEmail.update { contactEmail }
                    inviteContactResultChannel.trySend(triggered(result))
                }.onFailure { throwable ->
                    Timber.e(throwable)
                    inviteContactResultChannel.trySend(triggered(InviteContactRequest.InvalidStatus))
                }
        }
    }

    /**
     * Notify that [ScanQrCodeUiState.scannedContactLinkResult] has been consumed.
     */
    fun onScannedContactLinkResultConsumed() {
        scannedContactLinkResultChannel.trySend(consumed())
    }

    /**
     * Notify that [ScanQrCodeUiState.inviteContactResult] has been consumed.
     */
    fun onInviteContactResultConsumed() {
        inviteContactResultChannel.trySend(consumed())
    }

    /**
     * Notify that [ScanQrCodeUiState.scanCancel] has been consumed.
     */
    fun onScanCancelConsumed() {
        scanCancelChannel.trySend(consumed)
    }

    /**
     * Notify that [ScanQrCodeUiState.message] has been consumed.
     */
    fun onMessageConsumed() {
        messageChannel.trySend(consumed())
    }

    /**
     * Clear the stored scanned contact email.
     */
    fun resetScannedContactEmail() {
        scannedContactEmail.update { null }
    }

    /**
     * Clear the stored scanned contact avatar.
     */
    fun resetScannedContactAvatar() {
        scannedContactAvatar.update { null }
    }

    private suspend fun queryScannedContact(scannedHandle: String) {
        runCatching { queryScannedContactLinkUseCase(scannedHandle) }
            .onSuccess { result ->
                val avatar = avatarContentMapper(
                    fullName = result.contactName,
                    localFile = result.avatarFile,
                    showBorder = false,
                    textSize = SCANNED_AVATAR_TEXT_SIZE.sp,
                    backgroundColor = result.avatarColor ?: DEFAULT_AVATAR_BACKGROUND_COLOR,
                )
                scannedContactAvatar.update { avatar }
                scannedContactLinkResultChannel.trySend(triggered(result))
            }
            .onFailure {
                Timber.e(it)
                messageChannel.trySend(triggered(ScanQrCodeMessage.GeneralError))
            }
    }

    companion object {
        // Matches the legacy QRCode screen's scanned-contact avatar sizing.
        private const val SCANNED_AVATAR_TEXT_SIZE = 36f

        // Fallback avatar background when the scanned contact has no colour of its own.
        private val DEFAULT_AVATAR_BACKGROUND_COLOR = 0xFFF30C14.toInt()
    }
}
