package mega.privacy.android.feature.contact.qrcode.mycode

import androidx.compose.ui.unit.sp
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
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
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeError
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState
import mega.privacy.android.shared.account.presentation.mapper.AvatarContentMapper
import mega.privacy.android.shared.account.presentation.model.PhotoAvatarContent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyQrCodeViewModelTest {

    private lateinit var underTest: MyQrCodeViewModel

    private val copyToClipBoard = mock<CopyToClipBoard>()
    private val createContactLinkUseCase = mock<CreateContactLinkUseCase>()
    private val resetContactLinkUseCase = mock<ResetContactLinkUseCase>()
    private val deleteQRCodeUseCase = mock<DeleteQRCodeUseCase>()
    private val getQRCodeFileUseCase = mock<GetQRCodeFileUseCase>()
    private val getUserFullNameUseCase = mock<GetUserFullNameUseCase>()
    private val getMyAvatarColorUseCase = mock<GetMyAvatarColorUseCase>()
    private val getMyAvatarFileUseCase = mock<GetMyAvatarFileUseCase>()
    private val saveFileToDestinationUseCase = mock<SaveFileToDestinationUseCase>()
    private val doesUriPathHaveSufficientSpaceUseCase =
        mock<DoesUriPathHaveSufficientSpaceUseCase>()
    private val getPathByDocumentContentUriUseCase = mock<GetPathByDocumentContentUriUseCase>()
    private val avatarContentMapper = mock<AvatarContentMapper>()
    private val myQrCodeErrorMapper = mock<MyQrCodeErrorMapper>()

    private val initialContactLink = "https://contact_link1"

    @BeforeEach
    fun setUp() {
        underTest = MyQrCodeViewModel(
            copyToClipBoard = copyToClipBoard,
            createContactLinkUseCase = createContactLinkUseCase,
            resetContactLinkUseCase = resetContactLinkUseCase,
            deleteQRCodeUseCase = deleteQRCodeUseCase,
            getQRCodeFileUseCase = getQRCodeFileUseCase,
            getUserFullNameUseCase = getUserFullNameUseCase,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            saveFileToDestinationUseCase = saveFileToDestinationUseCase,
            doesUriPathHaveSufficientSpaceUseCase = doesUriPathHaveSufficientSpaceUseCase,
            getPathByDocumentContentUriUseCase = getPathByDocumentContentUriUseCase,
            avatarContentMapper = avatarContentMapper,
            myQrCodeErrorMapper = myQrCodeErrorMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            copyToClipBoard,
            createContactLinkUseCase,
            resetContactLinkUseCase,
            deleteQRCodeUseCase,
            getQRCodeFileUseCase,
            getUserFullNameUseCase,
            getMyAvatarColorUseCase,
            getMyAvatarFileUseCase,
            saveFileToDestinationUseCase,
            doesUriPathHaveSufficientSpaceUseCase,
            getPathByDocumentContentUriUseCase,
            avatarContentMapper,
            myQrCodeErrorMapper,
        )
    }

    @Test
    fun `test that initial state is CreatingQRCode`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(MyQrCodeUiState.CreatingQRCode)
    }

    @Test
    fun `test that init does not call createContactLinkUseCase before the state is collected`() =
        runTest {
            underTest.uiState

            verify(createContactLinkUseCase, never()).invoke(any())
        }

    @Test
    fun `test that init emits QRCodeAvailable when creation succeeds`() = runTest {
        stubQRCodeCreation()

        underTest.uiState.test {
            val state = awaitAvailableState()
            assertThat(state.qrCode.contactLink).isEqualTo(initialContactLink)
        }
    }

    @Test
    fun `test that init emits QRCodeUnavailable when creation fails`() = runTest {
        stubQRCodeCreationFailure()

        underTest.uiState.test {
            assertThat(awaitUnavailableState()).isInstanceOf(MyQrCodeUiState.QRCodeUnavailable::class.java)
        }
    }

    @Test
    fun `test that init emits GeneralError when creation fails`() = runTest {
        stubQRCodeCreationFailure()

        underTest.uiState.test {
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.GeneralError)
        }
    }

    @Test
    fun `test that createQRCode emits QRCodeAvailable when creation succeeds after an initial failure`() =
        runTest {
            stubQRCodeCreation()
            whenever(createContactLinkUseCase(renew = false))
                .thenAnswer { throw RuntimeException() }
                .thenReturn(initialContactLink)

            underTest.uiState.test {
                awaitUnavailableState()
                underTest.createQRCode()
                val state = awaitAvailableState()
                assertThat(state.qrCode.contactLink).isEqualTo(initialContactLink)
            }
        }

    @Test
    fun `test that createQRCode emits CreatingQRCode while creation is in flight`() = runTest {
        stubQRCodeCreation()
        val gate = CompletableDeferred<String>()
        whenever { createContactLinkUseCase(renew = false) } doSuspendableAnswer { gate.await() }

        underTest.uiState.test {
            assertThat(awaitState { it is MyQrCodeUiState.CreatingQRCode })
                .isEqualTo(MyQrCodeUiState.CreatingQRCode)

            gate.complete(initialContactLink)

            val state = awaitAvailableState()
            assertThat(state.qrCode.contactLink).isEqualTo(initialContactLink)
        }
    }

    @Test
    fun `test that createQRCode emits CreatingQRCode while re-creating after a delete`() = runTest {
        stubQRCodeCreation()

        underTest.uiState.test {
            awaitAvailableState()
            underTest.deleteQRCode(initialContactLink)
            awaitUnavailableState()

            val gate = CompletableDeferred<String>()
            whenever { createContactLinkUseCase(renew = false) } doSuspendableAnswer { gate.await() }
            underTest.createQRCode()
            assertThat(awaitState { it is MyQrCodeUiState.CreatingQRCode })
                .isEqualTo(MyQrCodeUiState.CreatingQRCode)

            gate.complete(initialContactLink)
            awaitAvailableState()
        }
    }

    @Test
    fun `test that resetQRCode emits CreatingQRCode while the reset is in flight`() = runTest {
        stubQRCodeCreation()
        val newContactLink = "https://contact_link2"
        val gate = CompletableDeferred<String>()
        whenever { resetContactLinkUseCase() } doSuspendableAnswer { gate.await() }

        underTest.uiState.test {
            awaitAvailableState()
            underTest.resetQRCode()
            assertThat(awaitState { it is MyQrCodeUiState.CreatingQRCode })
                .isEqualTo(MyQrCodeUiState.CreatingQRCode)

            gate.complete(newContactLink)

            val state = awaitAvailableStateWithLink(newContactLink)
            assertThat(state.qrCode.contactLink).isEqualTo(newContactLink)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetQRCode emits QRCodeAvailable with the new link when reset succeeds`() =
        runTest {
            stubQRCodeCreation()
            val newContactLink = "https://contact_link2"
            whenever(resetContactLinkUseCase()).thenReturn(newContactLink)

            underTest.uiState.test {
                awaitAvailableState()
                underTest.resetQRCode()
                val state = awaitAvailableStateWithLink(newContactLink)
                assertThat(state.qrCode.contactLink).isEqualTo(newContactLink)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetQRCode retains the previous QR code when reset fails`() = runTest {
        stubQRCodeCreation()
        whenever(resetContactLinkUseCase()).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.ResetFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.resetQRCode()
            val retained = awaitFailureRetainedState()
            assertThat(retained.qrCode.contactLink).isEqualTo(initialContactLink)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetQRCode emits ResetDone when reset succeeds`() = runTest {
        stubQRCodeCreation()
        whenever(resetContactLinkUseCase()).thenReturn("https://contact_link2")

        underTest.uiState.test {
            awaitAvailableState()
            underTest.resetQRCode()
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.ResetDone)
        }
    }

    @Test
    fun `test that resetQRCode emits OperationError when reset fails`() = runTest {
        stubQRCodeCreation()
        whenever(resetContactLinkUseCase()).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.ResetFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.resetQRCode()
            assertThat(awaitMessage())
                .isEqualTo(MyQrCodeMessage.OperationError(MyQrCodeError.ResetFailed))
        }
    }

    @Test
    fun `test that resetQRCode stays QRCodeUnavailable when reset fails and no QR code is available`() =
        runTest {
            stubQRCodeCreationFailure()
            whenever(resetContactLinkUseCase()).thenAnswer { throw RuntimeException() }
            whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.ResetFailed)

            underTest.uiState.test {
                settleAfterCreationFailure()
                underTest.resetQRCode()
                val settled = awaitUnavailableStateWithMessage()
                assertThat((settled.message as StateEventWithContentTriggered).content)
                    .isEqualTo(MyQrCodeMessage.OperationError(MyQrCodeError.ResetFailed))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that deleteQRCode emits QRCodeUnavailable when deletion succeeds`() = runTest {
        stubQRCodeCreation()

        underTest.uiState.test {
            awaitAvailableState()
            underTest.deleteQRCode(initialContactLink)
            awaitUnavailableState()
        }
        verify(deleteQRCodeUseCase).invoke(contactLink = initialContactLink)
    }

    @Test
    fun `test that deleteQRCode retains the previous QR code when deletion fails`() = runTest {
        stubQRCodeCreation()
        whenever(deleteQRCodeUseCase(any())).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.DeleteFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.deleteQRCode(initialContactLink)
            val retained = awaitFailureRetainedState()
            assertThat(retained.qrCode.contactLink).isEqualTo(initialContactLink)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that deleteQRCode emits OperationError when deletion fails`() = runTest {
        stubQRCodeCreation()
        whenever(deleteQRCodeUseCase(any())).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.DeleteFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.deleteQRCode(initialContactLink)
            assertThat(awaitMessage())
                .isEqualTo(MyQrCodeMessage.OperationError(MyQrCodeError.DeleteFailed))
        }
    }

    @Test
    fun `test that copyContactLink copies the link and emits ContactLinkCopied`() = runTest {
        stubQRCodeCreation()

        underTest.uiState.test {
            awaitAvailableState()
            underTest.copyContactLink(initialContactLink)
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.ContactLinkCopied)
        }
        verify(copyToClipBoard).invoke(label = "contact link", text = initialContactLink)
    }

    @Test
    fun `test that saveToFileSystem emits SaveError when QR file is null`() = runTest {
        stubQRCodeCreationFailure()
        whenever(getQRCodeFileUseCase()).thenReturn(null)

        underTest.uiState.test {
            settleAfterCreationFailure()
            underTest.saveToFileSystem(UriPath("/test/destination"))
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.SaveError)
        }
    }

    @Test
    fun `test that saveToFileSystem emits NotEnoughSpace when space is insufficient`() = runTest {
        stubQRCodeCreationFailure()
        val destination = UriPath("/test/destination")
        val qrFile = mock<File> { on { length() } doReturn 1000L }
        whenever(getQRCodeFileUseCase()).thenReturn(qrFile)
        whenever(doesUriPathHaveSufficientSpaceUseCase(destination, 1000L)).thenReturn(false)

        underTest.uiState.test {
            settleAfterCreationFailure()
            underTest.saveToFileSystem(destination)
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.NotEnoughSpace)
        }
    }

    @Test
    fun `test that saveToFileSystem emits SaveSuccess with the display path when save succeeds`() =
        runTest {
            stubQRCodeCreationFailure()
            val destination = UriPath("content://downloads/tree")
            val displayPath = "/storage/emulated/0/Download"
            val qrFile = mock<File> {
                on { length() } doReturn 1000L
                on { name } doReturn "qr_code.png"
            }
            whenever(getQRCodeFileUseCase()).thenReturn(qrFile)
            whenever(doesUriPathHaveSufficientSpaceUseCase(destination, 1000L)).thenReturn(true)
            whenever(getPathByDocumentContentUriUseCase(destination.value)).thenReturn(displayPath)

            underTest.uiState.test {
                settleAfterCreationFailure()
                underTest.saveToFileSystem(destination)
                assertThat(awaitMessage())
                    .isEqualTo(MyQrCodeMessage.SaveSuccess(displayPath))
            }
        }

    @Test
    fun `test that saveToFileSystem emits GeneralError when saving throws IOException`() = runTest {
        stubQRCodeCreationFailure()
        val destination = UriPath("/test/destination")
        val qrFile = mock<File> { on { length() } doReturn 1000L }
        whenever(getQRCodeFileUseCase()).thenReturn(qrFile)
        whenever(doesUriPathHaveSufficientSpaceUseCase(destination, 1000L)).thenReturn(true)
        whenever(getPathByDocumentContentUriUseCase(destination.value)).thenReturn("/test/path")
        whenever(saveFileToDestinationUseCase(any(), any())).thenAnswer { throw IOException("io") }

        underTest.uiState.test {
            settleAfterCreationFailure()
            underTest.saveToFileSystem(destination)
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.GeneralError)
        }
    }

    @Test
    fun `test that onMessageConsumed clears the message`() = runTest {
        stubQRCodeCreation()

        underTest.uiState.test {
            awaitAvailableState()
            underTest.copyContactLink(initialContactLink)
            awaitMessage()
            underTest.onMessageConsumed()
            val state = awaitConsumedMessageState()
            assertThat(state.message).isEqualTo(consumed())
        }
    }

    private suspend fun stubQRCodeCreation() {
        val localAvatarFile = mock<File> {
            on { exists() } doReturn true
            on { length() } doReturn 100L
        }
        val qrFile = mock<File> { on { absolutePath } doReturn "/test/qr.png" }

        whenever(createContactLinkUseCase(renew = false)).thenReturn(initialContactLink)
        whenever(getUserFullNameUseCase(any())).thenReturn("FullName")
        whenever(getMyAvatarColorUseCase()).thenReturn(0xFFFFF)
        whenever(getMyAvatarFileUseCase(any())).thenReturn(localAvatarFile)
        whenever(getQRCodeFileUseCase()).thenReturn(qrFile)
        whenever(
            avatarContentMapper.invoke(
                fullName = "FullName",
                localFile = localAvatarFile,
                showBorder = true,
                textSize = 38.sp,
                backgroundColor = 0xFFFFF,
            )
        ).thenReturn(PhotoAvatarContent(path = "photo_path", size = 1L, showBorder = true))
    }

    private suspend fun stubQRCodeCreationFailure() {
        whenever(createContactLinkUseCase(renew = false)).thenAnswer { throw RuntimeException() }
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.settleAfterCreationFailure() {
        awaitMessage()
        underTest.onMessageConsumed()
        awaitConsumedMessageState()
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitAvailableState(): MyQrCodeUiState.QRCodeAvailable =
        awaitState { it is MyQrCodeUiState.QRCodeAvailable } as MyQrCodeUiState.QRCodeAvailable

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitAvailableStateWithLink(
        link: String,
    ): MyQrCodeUiState.QRCodeAvailable =
        awaitState {
            (it as? MyQrCodeUiState.QRCodeAvailable)?.qrCode?.contactLink == link
        } as MyQrCodeUiState.QRCodeAvailable

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitUnavailableState(): MyQrCodeUiState.QRCodeUnavailable =
        awaitState { it is MyQrCodeUiState.QRCodeUnavailable } as MyQrCodeUiState.QRCodeUnavailable

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitUnavailableStateWithMessage(): MyQrCodeUiState.QRCodeUnavailable =
        awaitState {
            it is MyQrCodeUiState.QRCodeUnavailable && it.message is StateEventWithContentTriggered
        } as MyQrCodeUiState.QRCodeUnavailable

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitFailureRetainedState(): MyQrCodeUiState.QRCodeAvailable =
        awaitState {
            it is MyQrCodeUiState.QRCodeAvailable && it.message is StateEventWithContentTriggered
        } as MyQrCodeUiState.QRCodeAvailable

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitConsumedMessageState(): MyQrCodeUiState =
        awaitState { it.message !is StateEventWithContentTriggered }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitMessage(): MyQrCodeMessage =
        (awaitState { it.message is StateEventWithContentTriggered }
            .message as StateEventWithContentTriggered).content

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitState(
        predicate: (MyQrCodeUiState) -> Boolean,
    ): MyQrCodeUiState {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
        return item
    }
}
