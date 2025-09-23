package mega.privacy.android.app.presentation.qrcode

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.model.QRCodeUIState
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.transferoverquota.TransferOverQuotaViewModel
import mega.privacy.android.app.presentation.transfers.transferoverquota.model.TransferOverQuotaViewState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class QRCodeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val qrCodeMapper: QRCodeMapper = mock()
    private val startTransfersComponentViewModel = mock<StartTransfersComponentViewModel> {
        on { uiState } doReturn MutableStateFlow(StartTransferViewState())
    }
    private val transfersOverQuotaViewModel = mock<TransferOverQuotaViewModel> {
        on { uiState } doReturn MutableStateFlow(TransferOverQuotaViewState())
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(StartTransfersComponentViewModel::class.java.canonicalName.orEmpty()) }) } doReturn startTransfersComponentViewModel
        on { get(argThat<String> { contains(TransferOverQuotaViewModel::class.java.canonicalName.orEmpty()) }) } doReturn transfersOverQuotaViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    private fun setComposeContent(viewState: QRCodeUIState = QRCodeUIState()) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                QRCodeView(
                    viewState = viewState,
                    onBackPressed = { },
                    onCreateQRCode = { },
                    onDeleteQRCode = { },
                    onResetQRCode = { },
                    onScanQrCodeClicked = { },
                    onCopyLinkClicked = { },
                    onViewContactClicked = { },
                    onInviteContactClicked = { _, _ -> },
                    onResultMessageConsumed = { },
                    onScannedContactLinkResultConsumed = { },
                    onInviteContactResultConsumed = { },
                    onInviteResultDialogDismiss = { },
                    onInviteContactDialogDismiss = { },
                    onCloudDriveClicked = { },
                    onFileSystemClicked = { },
                    onShowCollision = { },
                    onShowCollisionConsumed = { },
                    onUploadFile = { },
                    onUploadFileConsumed = { },
                    onScanCancelConsumed = { },
                    onUploadEventConsumed = {},
                    navigateToQrSettings = {},
                    qrCodeMapper = qrCodeMapper,
                )
            }
        }
    }

    private suspend fun prepareQRCodeMapper() {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        whenever(qrCodeMapper(any(), any(), any(), any(), any())).thenReturn(dummyBitmap)
    }

    @Test
    fun `test that QR code and copy link button is shown when myQRCodeState is QRCodeAvailable`() =
        runTest {
            val expectedContactLink = "http://contact_link"
            val uiState = MyCodeUIState.QRCodeAvailable(
                contactLink = expectedContactLink,
                avatarBgColor = Color.Red.toArgb(),
                avatarContent = TextAvatarContent("Jack", Color.Black.toArgb())
            )
            prepareQRCodeMapper()
            setComposeContent(QRCodeUIState(myQRCodeState = uiState))
            composeTestRule.onNodeWithTag(QRCODE_TAG).assertIsDisplayed()
            composeTestRule.onNodeWithTag(LINK_TAG).assertIsDisplayed()
        }

    @Test
    fun `test that QR code bitmap and copy link button is not shown when myQRCodeState is Idle`() =
        runTest {
            setComposeContent(QRCodeUIState(myQRCodeState = MyCodeUIState.Idle))
            composeTestRule.onNodeWithContentDescription(label = QRCODE_TAG).assertDoesNotExist()
            composeTestRule.onNodeWithContentDescription(label = LINK_TAG).assertDoesNotExist()
        }

    @Test
    fun `test that scan button is always shown`() {
        setComposeContent(QRCodeUIState(myQRCodeState = MyCodeUIState.Idle))
        composeTestRule.onNodeWithTag(SCAN_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar message is shown when myQRCodeState is QRCodeResetDone`() {
        setComposeContent(QRCodeUIState(myQRCodeState = MyCodeUIState.QRCodeResetDone))
        composeTestRule.onNodeWithTag(SNACKBAR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar message is shown when myQRCodeState is QRCodeDeleted`() {
        setComposeContent(QRCodeUIState(myQRCodeState = MyCodeUIState.QRCodeDeleted))
        composeTestRule.onNodeWithTag(SNACKBAR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that create button is shown when myQRCodeState is QRCodeDeleted`() {
        setComposeContent(QRCodeUIState(myQRCodeState = MyCodeUIState.QRCodeDeleted))
        composeTestRule.onNodeWithTag(CREATE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that create button is shown when myQRCodeState is Idle`() {
        setComposeContent(QRCodeUIState(myQRCodeState = MyCodeUIState.Idle))
        composeTestRule.onNodeWithTag(CREATE_TAG).assertIsDisplayed()
    }
}