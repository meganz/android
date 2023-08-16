package test.mega.privacy.android.app.presentation.qrcode

import android.content.Context
import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.mapper.AvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.CombineQRCodeAndAvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.GetCircleBitmapMapper
import mega.privacy.android.app.presentation.qrcode.mapper.LoadBitmapFromFileMapper
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mapper.SaveBitmapToFileMapper
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeViewModel
import mega.privacy.android.domain.usecase.CopyToClipBoard
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import mega.privacy.android.domain.usecase.qrcode.DeleteQRCodeUseCase
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.qrcode.ResetContactLinkUseCase
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Test cases for [MyCodeViewModel]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MyCodeViewModelTest {

    private lateinit var underTest: MyCodeViewModel

    private val copyToClipBoard = mock<CopyToClipBoard>()
    private val createContactLinkUseCase: CreateContactLinkUseCase = mock()
    private val deleteQRCodeUseCase: DeleteQRCodeUseCase = mock()
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase = mock()
    private val resetContactLinkUseCase: ResetContactLinkUseCase = mock()
    private val qrCodeMapper: QRCodeMapper = mock()
    private val avatarMapper: AvatarMapper = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock()
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val getCurrentUserFullName: GetCurrentUserFullName = mock()
    private val context: Context = mock()
    private val loadBitmapFromFileMapper: LoadBitmapFromFileMapper = mock()
    private val saveBitmapToFileMapper: SaveBitmapToFileMapper = mock()
    private val getCircleBitmapMapper: GetCircleBitmapMapper = mock()
    private val combineQRCodeAndAvatarMapper: CombineQRCodeAndAvatarMapper = mock()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val penColor = 0xFF00000
    private val bgColor = 0xFFFFFF
    private val avatarBorderColor = 0x0000FF
    private val qrCodeWidth = 100
    private val qrCodeHeight = 100
    private val avatarWidth = 100
    private val avatarBorderWidth = 3

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = MyCodeViewModel(
            context = context,
            copyToClipBoard = copyToClipBoard,
            createContactLinkUseCase = createContactLinkUseCase,
            getQRCodeFileUseCase = getQRCodeFileUseCase,
            deleteQRCodeUseCase = deleteQRCodeUseCase,
            resetContactLinkUseCase = resetContactLinkUseCase,
            qrCodeMapper = qrCodeMapper,
            avatarMapper = avatarMapper,
            getMyAvatarColorUseCase = getMyAvatarColorUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            getCurrentUserFullName = getCurrentUserFullName,
            loadBitmapFromFile = loadBitmapFromFileMapper,
            saveBitmapToFile = saveBitmapToFileMapper,
            getCircleBitmap = getCircleBitmapMapper,
            combineQRCodeAndAvatar = combineQRCodeAndAvatarMapper,
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            with(initialState) {
                assertThat(contactLink).isNull()
                assertThat(qrCodeBitmap).isNull()
                assertThat(isInProgress).isFalse()
                assertThat(snackBarMessage).isNull()
                assertThat(localQRCodeFile).isNull()
                assertThat(hasQRCodeBeenDeleted).isFalse()
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
    fun `test that message is shown when copy contact link is successful`() = runTest {
        prepareQRCode()
        underTest.copyContactLink()
        underTest.uiState.test {
            assertThat(awaitItem().snackBarMessage).isEqualTo(R.string.qrcode_link_copied)
        }
    }


    @Test
    fun `test that QR code file can be shared when it exists`() = runTest {
        val file: File = mock {
            on { exists() }.thenReturn(true)
        }
        whenever(getQRCodeFileUseCase()).thenReturn(file)
        underTest.startSharing()
        underTest.uiState.test {
            assertThat(awaitItem().localQRCodeFile).isEqualTo(file)
        }
    }

    @Test
    fun `test that QR code file is not shared when it does not exist`() = runTest {
        val file: File = mock {
            on { exists() }.thenReturn(false)
        }
        whenever(getQRCodeFileUseCase()).thenReturn(file)
        underTest.startSharing()
        underTest.uiState.test {
            assertThat(awaitItem().localQRCodeFile).isNull()
        }
    }

    @Test
    fun `test that exception is captured when getQR Code file throws exception when sharing`() =
        runTest {
            whenever(getQRCodeFileUseCase()).thenAnswer { throw Exception() }
            underTest.startSharing()
        }

    @Test
    fun `test that QR code can be generated from local cache`() = runTest {
        val localQRCodeFile = mock<File> {
            on { exists() }.thenReturn(true)
        }
        val bitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val contactLink = "https://contact_link"
        whenever(getQRCodeFileUseCase()).thenReturn(localQRCodeFile)
        whenever(loadBitmapFromFileMapper(localQRCodeFile)).thenReturn(bitmap)
        whenever(createContactLinkUseCase(any())).thenReturn(contactLink)
        underTest.createQRCode(
            width = qrCodeWidth,
            height = qrCodeHeight,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = avatarWidth,
            avatarBorderWidth = avatarBorderWidth,
            avatarBorderColor = avatarBorderColor,
        )
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(bitmap).isEqualTo(bitmap)
                assertThat(isInProgress).isFalse()
                assertThat(contactLink).isEqualTo(contactLink)
            }
        }
    }

    @Test
    fun `test that QR code can be generated from remote`() = runTest {
        val localQRCodeFile = mock<File> {
            on { exists() }.thenReturn(false)
        }
        val bitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val contactLink = "https://contact_link"
        whenever(getQRCodeFileUseCase()).thenReturn(localQRCodeFile)
        whenever(createContactLinkUseCase(any())).thenReturn(contactLink)
        whenever(combineQRCodeAndAvatarMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(bitmap)
        underTest.createQRCode(
            width = qrCodeWidth,
            height = qrCodeHeight,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = avatarWidth,
            avatarBorderWidth = avatarBorderWidth,
            avatarBorderColor = avatarBorderColor,
        )
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(bitmap).isEqualTo(bitmap)
                assertThat(isInProgress).isFalse()
                assertThat(contactLink).isEqualTo(contactLink)
            }
        }
    }

    @Test
    fun `test that ui state can be re-used when creating QR code`() = runTest {
        prepareQRCode()
        whenever(getQRCodeFileUseCase()).thenReturn(null)
        whenever(createContactLinkUseCase(any())).thenReturn(null)
        underTest.createQRCode(
            width = qrCodeWidth,
            height = qrCodeHeight,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = avatarWidth,
            avatarBorderWidth = avatarBorderWidth,
            avatarBorderColor = avatarBorderColor
        )
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(contactLink).isNotNull()
                assertThat(qrCodeBitmap).isNotNull()
            }
        }
    }

    @Test
    fun `test that QRCode can be reset successfully`() = runTest {
        prepareQRCode()
        val localAvatarFile = mock<File> {
            on { exists() }.thenReturn(true)
            on { length() }.thenReturn(100)
        }
        val tmpBitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val expectedQrCodeBitmap =
            Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val contactLink = "https://contact_link2"
        whenever(resetContactLinkUseCase()).thenReturn(contactLink)
        whenever(qrCodeMapper(any(), any(), any(), any(), any())).thenReturn(tmpBitmap)
        whenever(getCurrentUserFullName(any(), any(), any())).thenReturn("fullname")
        whenever(loadBitmapFromFileMapper(any())).thenReturn(tmpBitmap)
        whenever(getCircleBitmapMapper(any())).thenReturn(tmpBitmap)
        whenever(combineQRCodeAndAvatarMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(expectedQrCodeBitmap)
        whenever(context.getString(any())).thenReturn("first name").thenReturn("last name")
        whenever(getMyAvatarFileUseCase(isForceRefresh = false)).thenReturn(localAvatarFile)

        // start real testing
        underTest.resetQRCode(
            width = qrCodeWidth,
            height = qrCodeHeight,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = avatarWidth,
            avatarBorderWidth = avatarBorderWidth,
            avatarBorderColor = avatarBorderColor
        )
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(qrCodeBitmap).isEqualTo(expectedQrCodeBitmap)
                assertThat(contactLink).isEqualTo(contactLink)
                assertThat(isInProgress).isFalse()
                assertThat(snackBarMessage).isEqualTo(R.string.qrcode_reset_successfully)
            }
        }
    }

    @Test
    fun `test that QR Code can be deleted successfully`() = runTest {
        prepareQRCode()

        underTest.deleteQR()
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(contactLink).isNull()
                assertThat(qrCodeBitmap).isNull()
                assertThat(hasQRCodeBeenDeleted).isTrue()
            }
        }
    }

    @Test
    fun `test that error message is shown when delete QR code fails`() = runTest {
        prepareQRCode()

        whenever(deleteQRCodeUseCase(any())).thenAnswer { throw Exception() }

        underTest.deleteQR()
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(snackBarMessage).isEqualTo(R.string.qrcode_delete_not_successfully)
            }
        }
    }

    @Test
    fun `test that QRCode reset fails when resetContactLink use case throws exception`() = runTest {
        prepareQRCode()

        whenever(resetContactLinkUseCase()).thenAnswer { throw Exception() }
        underTest.resetQRCode(
            width = qrCodeWidth,
            height = qrCodeHeight,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = avatarWidth,
            avatarBorderWidth = avatarBorderWidth,
            avatarBorderColor = avatarBorderColor,
        )
        underTest.uiState.test {
            val state = awaitItem()
            with(state) {
                assertThat(snackBarMessage).isEqualTo(R.string.qrcode_reset_not_successfully)
            }
        }
    }

    private suspend fun prepareQRCode() {
        val localQRCodeFile = mock<File> {
            on { exists() }.thenReturn(true)
        }
        val localAvatarFile = mock<File> {
            on { exists() }.thenReturn(true)
            on { length() }.thenReturn(100)
        }
        val bitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val tmpBitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val expectedQrCodeBitmap =
            Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.ARGB_8888)
        val contactLink = "https://contact_link1"
        whenever(getQRCodeFileUseCase()).thenReturn(localQRCodeFile)
        whenever(loadBitmapFromFileMapper(localQRCodeFile)).thenReturn(bitmap)
        whenever(createContactLinkUseCase(any())).thenReturn(contactLink)
        whenever(qrCodeMapper(any(), any(), any(), any(), any())).thenReturn(tmpBitmap)
        whenever(getCurrentUserFullName(any(), any(), any())).thenReturn("fullname")
        whenever(loadBitmapFromFileMapper(any())).thenReturn(tmpBitmap)
        whenever(getCircleBitmapMapper(any())).thenReturn(tmpBitmap)
        whenever(combineQRCodeAndAvatarMapper(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(expectedQrCodeBitmap)
        whenever(context.getString(any())).thenReturn("first name").thenReturn("last name")
        whenever(getMyAvatarFileUseCase(isForceRefresh = false)).thenReturn(localAvatarFile)
        underTest.createQRCode(
            width = qrCodeWidth,
            height = qrCodeHeight,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = avatarWidth,
            avatarBorderWidth = avatarBorderWidth,
            avatarBorderColor = avatarBorderColor,
        )
    }
}