package mega.privacy.android.feature.contact.qrcode.scan

import androidx.compose.ui.unit.sp
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanResult
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanner
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScannerModuleIsNotInstalled
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.qrcode.ParseScannedContactLinkHandleUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.feature.contact.qrcode.scan.model.ScanQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.scan.model.ScanQrCodeUiState
import mega.privacy.android.shared.account.presentation.mapper.AvatarContentMapper
import mega.privacy.android.shared.account.presentation.model.PhotoAvatarContent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanQrCodeViewModelTest {

    private lateinit var underTest: ScanQrCodeViewModel

    private val scannerHandler = mock<BarcodeScanner>()
    private val parseScannedContactLinkHandleUseCase = mock<ParseScannedContactLinkHandleUseCase>()
    private val queryScannedContactLinkUseCase = mock<QueryScannedContactLinkUseCase>()
    private val inviteContactWithHandleUseCase = mock<InviteContactWithHandleUseCase>()
    private val avatarContentMapper = mock<AvatarContentMapper>()

    @BeforeEach
    fun setUp() {
        underTest = ScanQrCodeViewModel(
            scannerHandler = scannerHandler,
            parseScannedContactLinkHandleUseCase = parseScannedContactLinkHandleUseCase,
            queryScannedContactLinkUseCase = queryScannedContactLinkUseCase,
            inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
            avatarContentMapper = avatarContentMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            scannerHandler,
            parseScannedContactLinkHandleUseCase,
            queryScannedContactLinkUseCase,
            inviteContactWithHandleUseCase,
            avatarContentMapper,
        )
    }

    @Test
    fun `test that initial state has no scanned contact and consumed events`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.scannedContactLinkResult).isEqualTo(consumed())
            assertThat(initial.scannedContactAvatarContent).isNull()
            assertThat(initial.scannedContactEmail).isNull()
            assertThat(initial.inviteContactResult).isEqualTo(consumed())
            assertThat(initial.scanCancel).isEqualTo(consumed)
            assertThat(initial.message).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that scanCode triggers scanCancel when scanning is cancelled`() = runTest {
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Cancelled)

        underTest.uiState.test {
            underTest.scanCode()
            val state = awaitState { it.scanCancel == triggered }
            assertThat(state.scanCancel).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that scanCode queries the parsed handle when a valid contact link is scanned`() =
        runTest {
            val contactLink = "https://mega.nz/C!wf8jTYRB"
            val handle = "wf8jTYRB"
            val scanResult = scannedContactLinkResult(avatarColor = 4040)
            whenever(scannerHandler.scanBarcode())
                .thenReturn(BarcodeScanResult.Success(contactLink))
            whenever(parseScannedContactLinkHandleUseCase(contactLink)).thenReturn(handle)
            whenever(queryScannedContactLinkUseCase(handle)).thenReturn(scanResult)
            stubAvatarMapper()

            underTest.uiState.test {
                underTest.scanCode()
                val state = awaitScannedResult()
                assertThat((state.scannedContactLinkResult as StateEventWithContentTriggered).content)
                    .isEqualTo(scanResult)
                assertThat(state.scannedContactAvatarContent).isNotNull()
            }
        }

    @Test
    fun `test that scanCode emits InvalidCode when the scanned value is not a contact link`() =
        runTest {
            val scannedCode = "https://mega.io/C!wf8jTYRB"
            whenever(scannerHandler.scanBarcode())
                .thenReturn(BarcodeScanResult.Success(scannedCode))
            whenever(parseScannedContactLinkHandleUseCase(scannedCode)).thenReturn(null)

            underTest.uiState.test {
                underTest.scanCode()
                assertThat(awaitMessage()).isEqualTo(ScanQrCodeMessage.InvalidCode)
            }
            verifyNoInteractions(queryScannedContactLinkUseCase)
        }

    @Test
    fun `test that scanCode emits InvalidCode when the scanned value is null`() = runTest {
        whenever(scannerHandler.scanBarcode())
            .thenReturn(BarcodeScanResult.Success(null))

        underTest.uiState.test {
            underTest.scanCode()
            assertThat(awaitMessage()).isEqualTo(ScanQrCodeMessage.InvalidCode)
        }
        verifyNoInteractions(parseScannedContactLinkHandleUseCase)
    }

    @Test
    fun `test that queryContactLink emits GeneralError when the query fails`() = runTest {
        val handle = "1234"
        whenever(queryScannedContactLinkUseCase(handle)).thenAnswer { throw RuntimeException() }

        underTest.uiState.test {
            underTest.queryContactLink(handle)
            assertThat(awaitMessage()).isEqualTo(ScanQrCodeMessage.GeneralError)
        }
    }

    @Test
    fun `test that scanCode is silent when the scanner module is not installed`() = runTest {
        whenever(scannerHandler.scanBarcode())
            .thenAnswer { throw BarcodeScannerModuleIsNotInstalled() }

        underTest.scanCode()

        underTest.uiState.test {
            assertThat(awaitItem().message).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that scanCode emits GeneralError when scanning fails unexpectedly`() = runTest {
        whenever(scannerHandler.scanBarcode()).thenAnswer { throw RuntimeException() }

        underTest.uiState.test {
            underTest.scanCode()
            assertThat(awaitMessage()).isEqualTo(ScanQrCodeMessage.GeneralError)
        }
    }

    @Test
    fun `test that queryContactLink sets the scanned result and avatar`() = runTest {
        val handle = "1234"
        val scanResult = scannedContactLinkResult(avatarColor = 4040)
        whenever(queryScannedContactLinkUseCase(handle)).thenReturn(scanResult)
        stubAvatarMapper()

        underTest.uiState.test {
            underTest.queryContactLink(handle)
            val state = awaitScannedResult()
            assertThat((state.scannedContactLinkResult as StateEventWithContentTriggered).content)
                .isEqualTo(scanResult)
            assertThat(state.scannedContactAvatarContent).isNotNull()
        }
    }

    @Test
    fun `test that sendInvite triggers inviteContactResult and sets the email when it succeeds`() =
        runTest {
            whenever(inviteContactWithHandleUseCase(any(), any(), anyOrNull()))
                .thenReturn(InviteContactRequest.Sent)

            underTest.uiState.test {
                underTest.sendInvite(123L, "abc@gmail.com")
                val state = awaitState { it.inviteContactResult is StateEventWithContentTriggered }
                assertThat((state.inviteContactResult as StateEventWithContentTriggered).content)
                    .isEqualTo(InviteContactRequest.Sent)
                assertThat(state.scannedContactEmail).isEqualTo("abc@gmail.com")
            }
        }

    @Test
    fun `test that sendInvite triggers InvalidStatus when it throws`() = runTest {
        whenever(inviteContactWithHandleUseCase(any(), any(), anyOrNull()))
            .thenAnswer { throw RuntimeException() }

        underTest.uiState.test {
            underTest.sendInvite(123L, "abc@gmail.com")
            val state = awaitState { it.inviteContactResult is StateEventWithContentTriggered }
            assertThat((state.inviteContactResult as StateEventWithContentTriggered).content)
                .isEqualTo(InviteContactRequest.InvalidStatus)
        }
    }

    @Test
    fun `test that resetScannedContactEmail clears the email`() = runTest {
        whenever(inviteContactWithHandleUseCase(any(), any(), anyOrNull()))
            .thenReturn(InviteContactRequest.Sent)

        underTest.uiState.test {
            underTest.sendInvite(123L, "abc@gmail.com")
            awaitState { it.scannedContactEmail == "abc@gmail.com" }
            underTest.resetScannedContactEmail()
            val state = awaitState { it.scannedContactEmail == null }
            assertThat(state.scannedContactEmail).isNull()
        }
    }

    @Test
    fun `test that resetScannedContactAvatar clears the avatar`() = runTest {
        val handle = "1234"
        whenever(queryScannedContactLinkUseCase(handle))
            .thenReturn(scannedContactLinkResult(avatarColor = 4040))
        stubAvatarMapper()

        underTest.uiState.test {
            underTest.queryContactLink(handle)
            awaitState { it.scannedContactAvatarContent != null }
            underTest.resetScannedContactAvatar()
            val state = awaitState { it.scannedContactAvatarContent == null }
            assertThat(state.scannedContactAvatarContent).isNull()
        }
    }

    @Test
    fun `test that onScanCancelConsumed clears the scanCancel event`() = runTest {
        whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Cancelled)

        underTest.uiState.test {
            underTest.scanCode()
            awaitState { it.scanCancel == triggered }
            underTest.onScanCancelConsumed()
            val state = awaitState { it.scanCancel == consumed }
            assertThat(state.scanCancel).isEqualTo(consumed)
        }
    }

    private fun scannedContactLinkResult(avatarColor: Int?) = ScannedContactLinkResult(
        contactName = "abc",
        email = "abc@gmail.com",
        handle = 12345,
        isContact = false,
        qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK,
        avatarFile = null,
        avatarColor = avatarColor,
    )

    private suspend fun stubAvatarMapper() {
        whenever(
            avatarContentMapper.invoke(
                fullName = "abc",
                localFile = null,
                showBorder = false,
                textSize = 36.sp,
                backgroundColor = 4040,
            )
        ).thenReturn(PhotoAvatarContent(path = "photo_path", size = 1L, showBorder = true))
    }

    private suspend fun ReceiveTurbine<ScanQrCodeUiState>.awaitState(
        predicate: (ScanQrCodeUiState) -> Boolean,
    ): ScanQrCodeUiState {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
        return item
    }

    private suspend fun ReceiveTurbine<ScanQrCodeUiState>.awaitScannedResult(): ScanQrCodeUiState =
        awaitState { it.scannedContactLinkResult is StateEventWithContentTriggered }

    private suspend fun ReceiveTurbine<ScanQrCodeUiState>.awaitMessage(): ScanQrCodeMessage {
        val state = awaitState { it.message is StateEventWithContentTriggered }
        return (state.message as StateEventWithContentTriggered).content
    }
}
