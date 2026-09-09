package mega.privacy.android.feature.contact.qrcode.mycode

import androidx.compose.ui.unit.sp
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
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
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeContentState
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
    fun `test that initial state is Idle with no message`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.contentState).isEqualTo(MyQrCodeContentState.Idle)
            assertThat(initial.message).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that createQRCode sets QRCodeAvailable when creation succeeds`() = runTest {
        stubQRCodeCreation()

        underTest.uiState.test {
            underTest.createQRCode(showLoader = true)
            val state = awaitAvailableState()
            assertThat(state.contactLink).isEqualTo(initialContactLink)
        }
    }

    @Test
    fun `test that createQRCode sets Idle when creation fails`() = runTest {
        whenever(createContactLinkUseCase(renew = false)).thenAnswer { throw RuntimeException() }

        underTest.createQRCode(showLoader = true)

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(MyQrCodeContentState.Idle)
        }
    }

    @Test
    fun `test that createQRCode emits GeneralError when creation fails`() = runTest {
        whenever(createContactLinkUseCase(renew = false)).thenAnswer { throw RuntimeException() }

        underTest.uiState.test {
            underTest.createQRCode(showLoader = true)
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.GeneralError)
        }
    }

    @Test
    fun `test that resetQRCode sets QRCodeAvailable with the new link when reset succeeds`() =
        runTest {
            prepareAvailableQRCode()
            val newContactLink = "https://contact_link2"
            whenever(resetContactLinkUseCase()).thenReturn(newContactLink)

            underTest.uiState.test {
                underTest.resetQRCode()
                val state = awaitAvailableStateWithLink(newContactLink)
                assertThat(state.contactLink).isEqualTo(newContactLink)
            }
        }

    @Test
    fun `test that resetQRCode restores the previous QR code when reset fails`() = runTest {
        prepareAvailableQRCode()
        whenever(resetContactLinkUseCase()).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.ResetFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.resetQRCode()
            val restored = awaitFailureRestoredState()
            assertThat(restored.contactLink).isEqualTo(initialContactLink)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetQRCode emits ResetDone when reset succeeds`() = runTest {
        prepareAvailableQRCode()
        val newContactLink = "https://contact_link2"
        whenever(resetContactLinkUseCase()).thenReturn(newContactLink)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.resetQRCode()
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.ResetDone)
        }
    }

    @Test
    fun `test that resetQRCode emits OperationError when reset fails`() = runTest {
        prepareAvailableQRCode()
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
    fun `test that resetQRCode sets Idle when reset fails and no QR code is available`() = runTest {
        whenever(resetContactLinkUseCase()).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.ResetFailed)

        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(MyQrCodeContentState.Idle)
            underTest.resetQRCode()
            val settled = awaitIdleStateWithMessage()
            assertThat(settled.contentState).isEqualTo(MyQrCodeContentState.Idle)
            assertThat((settled.message as StateEventWithContentTriggered).content)
                .isEqualTo(MyQrCodeMessage.OperationError(MyQrCodeError.ResetFailed))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that deleteQRCode sets QRCodeDeleted when deletion succeeds`() = runTest {
        prepareAvailableQRCode()

        underTest.uiState.test {
            underTest.deleteQRCode()
            val state = awaitContentState { it is MyQrCodeContentState.QRCodeDeleted }
            assertThat(state).isEqualTo(MyQrCodeContentState.QRCodeDeleted)
        }
    }

    @Test
    fun `test that deleteQRCode restores the previous QR code when deletion fails`() = runTest {
        prepareAvailableQRCode()
        whenever(deleteQRCodeUseCase(any())).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.DeleteFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.deleteQRCode()
            val restored = awaitFailureRestoredState()
            assertThat(restored.contactLink).isEqualTo(initialContactLink)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that deleteQRCode emits OperationError when deletion fails`() = runTest {
        prepareAvailableQRCode()
        whenever(deleteQRCodeUseCase(any())).thenAnswer { throw RuntimeException() }
        whenever(myQrCodeErrorMapper(any())).thenReturn(MyQrCodeError.DeleteFailed)

        underTest.uiState.test {
            awaitAvailableState()
            underTest.deleteQRCode()
            assertThat(awaitMessage())
                .isEqualTo(MyQrCodeMessage.OperationError(MyQrCodeError.DeleteFailed))
        }
    }

    @Test
    fun `test that deleteQRCode does nothing when no QR code is available`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().contentState).isEqualTo(MyQrCodeContentState.Idle)
            underTest.deleteQRCode()
            expectNoEvents()
        }
        verifyNoInteractions(deleteQRCodeUseCase)
    }

    @Test
    fun `test that copyContactLink copies the link and emits ContactLinkCopied when available`() =
        runTest {
            prepareAvailableQRCode()

            underTest.uiState.test {
                awaitAvailableState()
                underTest.copyContactLink()
                val message = awaitMessage()
                assertThat(message).isEqualTo(MyQrCodeMessage.ContactLinkCopied)
            }
            verify(copyToClipBoard).invoke(label = "contact link", text = initialContactLink)
        }

    @Test
    fun `test that copyContactLink does nothing when no QR code is available`() = runTest {
        underTest.copyContactLink()
        verifyNoInteractions(copyToClipBoard)
    }

    @Test
    fun `test that saveToFileSystem emits SaveError when QR file is null`() = runTest {
        whenever(getQRCodeFileUseCase()).thenReturn(null)

        underTest.uiState.test {
            underTest.saveToFileSystem(UriPath("/test/destination"))
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.SaveError)
        }
    }

    @Test
    fun `test that saveToFileSystem emits NotEnoughSpace when space is insufficient`() = runTest {
        val destination = UriPath("/test/destination")
        val qrFile = mock<File> { on { length() } doReturn 1000L }
        whenever(getQRCodeFileUseCase()).thenReturn(qrFile)
        whenever(doesUriPathHaveSufficientSpaceUseCase(destination, 1000L)).thenReturn(false)

        underTest.uiState.test {
            underTest.saveToFileSystem(destination)
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.NotEnoughSpace)
        }
    }

    @Test
    fun `test that saveToFileSystem emits SaveSuccess with the display path when save succeeds`() =
        runTest {
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
                underTest.saveToFileSystem(destination)
                assertThat(awaitMessage())
                    .isEqualTo(MyQrCodeMessage.SaveSuccess(displayPath))
            }
        }

    @Test
    fun `test that saveToFileSystem emits GeneralError when saving throws IOException`() = runTest {
        val destination = UriPath("/test/destination")
        val qrFile = mock<File> { on { length() } doReturn 1000L }
        whenever(getQRCodeFileUseCase()).thenReturn(qrFile)
        whenever(doesUriPathHaveSufficientSpaceUseCase(destination, 1000L)).thenReturn(true)
        whenever(getPathByDocumentContentUriUseCase(destination.value)).thenReturn("/test/path")
        whenever(saveFileToDestinationUseCase(any(), any())).thenAnswer { throw IOException("io") }

        underTest.uiState.test {
            underTest.saveToFileSystem(destination)
            assertThat(awaitMessage()).isEqualTo(MyQrCodeMessage.GeneralError)
        }
    }

    @Test
    fun `test that onMessageConsumed clears the message`() = runTest {
        prepareAvailableQRCode()

        underTest.uiState.test {
            awaitAvailableState()
            underTest.copyContactLink()
            awaitMessage()
            underTest.onMessageConsumed()
            val state = awaitItem()
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

    private suspend fun prepareAvailableQRCode() {
        stubQRCodeCreation()
        underTest.createQRCode(showLoader = true)
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitAvailableState(): MyQrCodeContentState.QRCodeAvailable {
        var item = awaitItem()
        while (item.contentState !is MyQrCodeContentState.QRCodeAvailable) {
            item = awaitItem()
        }
        return item.contentState as MyQrCodeContentState.QRCodeAvailable
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitAvailableStateWithLink(
        link: String,
    ): MyQrCodeContentState.QRCodeAvailable {
        var item = awaitItem()
        while ((item.contentState as? MyQrCodeContentState.QRCodeAvailable)?.contactLink != link) {
            item = awaitItem()
        }
        return item.contentState as MyQrCodeContentState.QRCodeAvailable
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitFailureRestoredState(): MyQrCodeContentState.QRCodeAvailable {
        var item = awaitItem()
        while (item.contentState !is MyQrCodeContentState.QRCodeAvailable ||
            item.message !is StateEventWithContentTriggered
        ) {
            item = awaitItem()
        }
        return item.contentState as MyQrCodeContentState.QRCodeAvailable
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitIdleStateWithMessage(): MyQrCodeUiState {
        var item = awaitItem()
        while (item.contentState != MyQrCodeContentState.Idle ||
            item.message !is StateEventWithContentTriggered
        ) {
            item = awaitItem()
        }
        return item
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitContentState(
        predicate: (MyQrCodeContentState) -> Boolean,
    ): MyQrCodeContentState {
        var item = awaitItem()
        while (!predicate(item.contentState)) {
            item = awaitItem()
        }
        return item.contentState
    }

    private suspend fun ReceiveTurbine<MyQrCodeUiState>.awaitMessage(): MyQrCodeMessage {
        var item = awaitItem()
        while (item.message !is StateEventWithContentTriggered) {
            item = awaitItem()
        }
        return (item.message as StateEventWithContentTriggered).content
    }
}
