package mega.privacy.android.app.presentation.qrcode

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.avatar.model.PhotoAvatarContent
import mega.privacy.android.app.presentation.qrcode.mapper.MyQRCodeTextErrorMapper
import mega.privacy.android.core.nodecomponents.scanner.BarcodeScanResult
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.data.worker.AreNotificationsEnabledUseCase
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.usecase.CopyToClipBoard
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.DeleteQRCodeUseCase
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.ResetContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Test cases for [QRCodeViewModel]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QRCodeViewModelTest {

    private lateinit var underTest: QRCodeViewModel

    private val copyToClipBoard = mock<CopyToClipBoard>()
    private val createContactLinkUseCase: CreateContactLinkUseCase = mock()
    private val deleteQRCodeUseCase: DeleteQRCodeUseCase = mock()
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase = mock()
    private val resetContactLinkUseCase: ResetContactLinkUseCase = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock()
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val getUserFullNameUseCase: GetUserFullNameUseCase = mock()
    private val queryScannedContactLinkUseCase = mock<QueryScannedContactLinkUseCase>()
    private val inviteContactWithHandleUseCase = mock<InviteContactWithHandleUseCase>()
    private val avatarContentMapper = mock<AvatarContentMapper>()
    private val myQRCodeTextErrorMapper = mock<MyQRCodeTextErrorMapper>()
    private val scannerHandler = mock<ScannerHandler>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val doesUriPathHaveSufficientSpaceUseCase =
        mock<DoesUriPathHaveSufficientSpaceUseCase>()
    private val scanMediaFileUseCase = mock<ScanMediaFileUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()
    private val checkFileNameCollisionsUseCase = mock<CheckFileNameCollisionsUseCase>()
    private val context = mock<Context>()
    private val areNotificationsEnabledUseCase: AreNotificationsEnabledUseCase = mock()
    private val notificationManager: NotificationManagerCompat = mock()
    private val transfersActionGroupFinishNotificationBuilder: TransfersActionGroupFinishNotificationBuilder =
        mock()

    private val initialContactLink = "https://contact_link1"

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = QRCodeViewModel(
            copyToClipBoard = copyToClipBoard,
            createContactLinkUseCase = createContactLinkUseCase,
            getQRCodeFileUseCase = getQRCodeFileUseCase,
            deleteQRCodeUseCase = deleteQRCodeUseCase,
            resetContactLinkUseCase = resetContactLinkUseCase,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getUserFullNameUseCase = getUserFullNameUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            queryScannedContactLinkUseCase = queryScannedContactLinkUseCase,
            inviteContactWithHandleUseCase = inviteContactWithHandleUseCase,
            avatarContentMapper = avatarContentMapper,
            myQRCodeTextErrorMapper = myQRCodeTextErrorMapper,
            scannerHandler = scannerHandler,
            getCurrentUserEmail = getCurrentUserEmail,
            doesUriPathHaveSufficientSpaceUseCase = doesUriPathHaveSufficientSpaceUseCase,
            scanMediaFileUseCase = scanMediaFileUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            checkFileNameCollisionsUseCase = checkFileNameCollisionsUseCase,
            areNotificationsEnabledUseCase = areNotificationsEnabledUseCase,
            notificationManager = notificationManager,
            transfersActionGroupFinishNotificationBuilder = transfersActionGroupFinishNotificationBuilder,
            context = context,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            with(initialState) {
                assertThat(myQRCodeState).isEqualTo(MyCodeUIState.Idle)
                assertThat(resultMessage).isInstanceOf(consumed().javaClass)
                assertThat(inviteContactResult).isInstanceOf(consumed().javaClass)
                assertThat(scannedContactLinkResult).isInstanceOf(consumed().javaClass)
                assertThat(uploadFile).isInstanceOf(consumed().javaClass)
                assertThat(showCollision).isInstanceOf(consumed().javaClass)
                assertThat(scannedContactEmail).isNull()
                assertThat(scannedContactAvatarContent).isNull()
            }
        }
    }

    @Test
    fun `test that no contact link text can be copied to clipboard when it is initial state`() =
        runTest {
            underTest.copyContactLink()
            verifyNoInteractions(copyToClipBoard)
        }

    @Test
    fun `test that bitmap can be used`() = runTest {
        val expectedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertThat(expectedBitmap.width).isEqualTo(100)
    }

    @Test
    fun `test that QRCode can be reset successfully`() = runTest {
        prepareQRCode()
        val localAvatarFile = mock<File> {
            on { exists() }.thenReturn(true)
            on { length() }.thenReturn(100)
        }
        val newContactLink = "https://contact_link2"
        whenever(resetContactLinkUseCase()).thenReturn(newContactLink)
        whenever(getMyAvatarFileUseCase(isForceRefresh = false)).thenReturn(localAvatarFile)
        whenever(myQRCodeTextErrorMapper(any())).thenReturn("error")

        underTest.resetQRCode()
        underTest.uiState.test {
            val result = awaitItem()
            assertThat(result.myQRCodeState).isInstanceOf(MyCodeUIState.QRCodeAvailable::class.java)
            assertThat((result.myQRCodeState as MyCodeUIState.QRCodeAvailable).contactLink)
                .isEqualTo(newContactLink)
        }
    }

    @Test
    fun `test that QRCode reset fails when resetContactLink use case throws exception`() = runTest {
        prepareQRCode()

        whenever(resetContactLinkUseCase()).thenAnswer { throw Exception() }
        whenever(myQRCodeTextErrorMapper(any())).thenReturn("error")
        underTest.resetQRCode()
        underTest.uiState.test {
            val result = awaitItem()
            assertThat((result.myQRCodeState as MyCodeUIState.QRCodeAvailable).contactLink)
                .isEqualTo(initialContactLink)
        }
    }

    @Test
    fun `test that QR Code can be deleted successfully`() = runTest {
        prepareQRCode()
        underTest.deleteQRCode()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.myQRCodeState).isInstanceOf(MyCodeUIState.QRCodeDeleted::class.java)
        }
    }

    @Test
    fun `test that error message is shown when delete QR code fails`() = runTest {
        prepareQRCode()

        whenever(deleteQRCodeUseCase(any())).thenAnswer { throw Exception() }
        whenever(myQRCodeTextErrorMapper(any())).thenReturn("error")

        underTest.deleteQRCode()
        underTest.uiState.test {
            val result = awaitItem()
            assertThat((result.myQRCodeState as MyCodeUIState.QRCodeAvailable).contactLink)
                .isEqualTo(initialContactLink)
        }
    }

    @Test
    fun `test that inviteContactResult is set correctly when send invite is success`() =
        runTest {
            whenever(inviteContactWithHandleUseCase(any(), any(), anyOrNull()))
                .thenReturn(InviteContactRequest.Sent)
            underTest.uiState.test {
                awaitItem()
                underTest.sendInvite(123L, "abc@gmail.com")
                val newValue = awaitItem()
                assertThat(newValue.inviteContactResult).isInstanceOf(triggered(InviteContactRequest.Sent).javaClass)
            }
        }

    @Test
    fun `test that inviteContactResult is set to InvalidStatus when send invite throws exception`() =
        runTest {
            whenever(inviteContactWithHandleUseCase(any(), any(), anyOrNull()))
                .thenAnswer { throw RuntimeException() }
            underTest.uiState.test {
                awaitItem()
                underTest.sendInvite(123L, "abc@gmail.com")
                val newValue = awaitItem()
                assertThat(newValue.inviteContactResult).isInstanceOf(triggered(InviteContactRequest.InvalidStatus).javaClass)
            }
        }

    @Test
    fun `test that scannedContactLinkResult is set correctly when queryContactLink is invoked`() =
        runTest {
            val handle = "1234"
            val name = "abc"
            val avatarFile = mock<File> {
                on { exists() }.thenReturn(true)
                on { length() }.thenReturn(100)
            }
            val avatarColor = 4040
            val scanResult = ScannedContactLinkResult(
                name,
                "abc@gmail.com",
                12345,
                false,
                QRCodeQueryResults.CONTACT_QUERY_OK,
                avatarFile,
                avatarColor
            )
            val avatar = PhotoAvatarContent(path = "photo_path", size = 1L, showBorder = true)
            whenever(avatarContentMapper(name, avatarFile, false, 36.sp, avatarColor))
                .thenReturn(avatar)

            whenever(queryScannedContactLinkUseCase(handle)).thenReturn(scanResult)
            underTest.queryContactLink(mock(), handle)
            underTest.uiState.test {
                val result = awaitItem()
                assertThat(result.scannedContactLinkResult).isInstanceOf(triggered(scanResult).javaClass)
                assertThat(result.scannedContactAvatarContent).isEqualTo(avatar)
            }
        }

    @Test
    fun `test that scannedContactAvatarContent is set to null when resetScannedContactAvatarContent is invoked`() =
        runTest {
            val handle = "1234"
            val name = "abc"
            val avatarFile = mock<File> {
                on { exists() }.thenReturn(true)
                on { length() }.thenReturn(100)
            }
            val avatarColor = 4040
            val scanResult = ScannedContactLinkResult(
                name,
                "abc@gmail.com",
                12345,
                false,
                QRCodeQueryResults.CONTACT_QUERY_OK,
                avatarFile,
                avatarColor
            )
            whenever(
                avatarContentMapper(name, avatarFile, false, 36.sp, avatarColor)
            ).thenReturn(PhotoAvatarContent(path = "photo_path", size = 1L, showBorder = true))

            whenever(queryScannedContactLinkUseCase(handle)).thenReturn(scanResult)
            underTest.queryContactLink(mock(), handle)
            underTest.uiState.test {
                underTest.resetScannedContactAvatar()
                val result = expectMostRecentItem()
                assertThat(result.scannedContactAvatarContent).isNull()
            }
        }

    @Test
    fun `test that scannedContactEmail is set to null when resetScannedContactEmail is invoked`() =
        runTest {
            whenever(inviteContactWithHandleUseCase(any(), any(), anyOrNull()))
                .thenReturn(InviteContactRequest.Sent)
            underTest.sendInvite(123L, "abc@gmail.com")
            underTest.uiState.test {
                underTest.resetScannedContactEmail()
                val result = expectMostRecentItem()
                assertThat(result.scannedContactEmail).isNull()
            }
        }

    @Test
    fun `test that scanCancel is set to triggered when BarcodeScanResult Cancelled is returned from scanCode`() =
        runTest {
            whenever(scannerHandler.scanBarcode()).thenReturn(BarcodeScanResult.Cancelled)
            underTest.scanCode(mock())
            underTest.uiState.test {
                val result = awaitItem()
                assertThat(result.scanCancel).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that scanCancel is set to consumed when resetScanCancel is invoked`() = runTest {
        underTest.uiState.test {
            underTest.resetScanCancel()
            val result = expectMostRecentItem()
            assertThat(result.scanCancel).isEqualTo(consumed)
        }
    }

    private suspend fun prepareQRCode() {
        val localAvatarFile = mock<File> {
            on { exists() }.thenReturn(true)
            on { length() }.thenReturn(100)
        }

        whenever(createContactLinkUseCase(any())).thenReturn(initialContactLink)
        whenever(getMyAvatarFileUseCase(any())).thenReturn(localAvatarFile)
        whenever(getUserFullNameUseCase(any())).thenReturn("FullName")
        whenever(getMyAvatarColorUseCase()).thenReturn(0xFFFFF)

        whenever(
            avatarContentMapper.invoke(
                fullName = "FullName",
                localFile = localAvatarFile,
                showBorder = true,
                textSize = 38.sp,
                backgroundColor = 0xFFFFF
            )
        ).thenReturn(PhotoAvatarContent(path = "photo_path", size = 1L, showBorder = true))
        underTest.createQRCode(true)
    }

    @Test
    fun `test that state is updated correctly if a file upload needs to start`() = runTest {
        val file = mock<File>()
        val message = "message"
        val parentHandle = 123L
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(file.absolutePath to null),
                NodeId(parentHandle),
                specificStartMessage = message,
            )
        )
        whenever(context.getString(any(), anyVararg())) doReturn message

        underTest.uiState.map { it.uploadEvent }.test {
            awaitItem()
            underTest.uploadFile(file, parentHandle)
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }
}