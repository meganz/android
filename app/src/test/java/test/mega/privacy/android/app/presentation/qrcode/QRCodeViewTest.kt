package test.mega.privacy.android.app.presentation.qrcode

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.qrcode.LINK_TAG
import mega.privacy.android.app.presentation.qrcode.QRCODE_TAG
import mega.privacy.android.app.presentation.qrcode.QRCodeView
import mega.privacy.android.app.presentation.qrcode.SCAN_TAG
import mega.privacy.android.app.presentation.qrcode.SNACKBAR_TAG
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.model.QRCodeUIState
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class QRCodeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val qrCodeMapper: QRCodeMapper = mock()

    private fun setComposeContent(viewState: QRCodeUIState = QRCodeUIState()) {
        composeTestRule.setContent {
            QRCodeView(
                viewState = viewState,
                onBackPressed = { },
                onDeleteQRCode = { },
                onResetQRCode = { },
                onSaveQRCode = { },
                onShareClicked = { },
                onScanQrCodeClicked = { },
                onCopyLinkClicked = { },
                onViewContactClicked = { },
                onInviteContactClicked = { _, _ -> },
                onResultMessageConsumed = { },
                onScannedContactLinkResultConsumed = { },
                onInviteContactResultConsumed = { },
                onInviteContactDialogDismiss = { },
                onInviteResultDialogDismiss = { },
                qrCodeMapper = qrCodeMapper
            )
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
}