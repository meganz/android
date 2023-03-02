package test.mega.privacy.android.app.presentation.qrcode.mycode.view

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.app.presentation.qrcode.mycode.view.MyQRCodeView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MyQRCodeViewTest {

    private val qrCodeMapper: QRCodeMapper = mock()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that contact link is shown when there is valid value`() = runTest {
        val expectedContactLink = "http://contact_link"
        val uiState = MyCodeUIState.QRCodeAvailable(
            contactLink = expectedContactLink,
            avatarBgColor = Color.Red.toArgb(),
            avatarContent = TextAvatarContent("Jack")
        )
        prepareQRCodeMapper()
        composeTestRule.setContent {
            MyQRCodeView(
                uiState = uiState,
                onButtonClicked = {},
                onScroll = {},
                qrCodeMapper = qrCodeMapper,
            )
        }
        composeTestRule.onNodeWithText(expectedContactLink).assertExists()
    }

    @Test
    fun `test that contact link text is not shown shown when ui state is Idle`() = runTest {
        val expectedContactLink = "http://contact_link"
        val uiState = MyCodeUIState.Idle
        prepareQRCodeMapper()
        composeTestRule.setContent {
            MyQRCodeView(
                uiState = uiState,
                onButtonClicked = {},
                onScroll = {},
                qrCodeMapper = qrCodeMapper,
            )
        }
        composeTestRule.onNodeWithText(expectedContactLink).assertDoesNotExist()
    }

    @Test
    fun `test that button text is copylink when contact link is available`() = runTest {
        val expectedContactLink = "http://contact_link"
        val uiState = MyCodeUIState.QRCodeAvailable(
            contactLink = expectedContactLink,
            avatarBgColor = Color.Red.toArgb(),
            avatarContent = TextAvatarContent("Jack")
        )
        prepareQRCodeMapper()
        composeTestRule.setContent {
            MyQRCodeView(
                uiState = uiState,
                onButtonClicked = {},
                onScroll = {},
                qrCodeMapper = qrCodeMapper,
            )
        }
        composeTestRule.onNodeWithText(text = "Copy link").assertExists()
    }

    @Test
    fun `test that button text is CreateQRCode when ui state is idle`() = runTest {
        val uiState = MyCodeUIState.Idle
        prepareQRCodeMapper()
        composeTestRule.setContent {
            MyQRCodeView(
                uiState = uiState,
                onButtonClicked = {},
                onScroll = {},
                qrCodeMapper = qrCodeMapper,
            )
        }
        composeTestRule.onNodeWithText(text = "Create QR code").assertExists()
    }

    @Test
    fun `test that QR code bitmap is not shown when ui state is idle`() = runTest {
        val uiState = MyCodeUIState.Idle
        prepareQRCodeMapper()
        composeTestRule.setContent {
            MyQRCodeView(
                uiState = uiState,
                onButtonClicked = {},
                onScroll = {},
                qrCodeMapper = qrCodeMapper,
            )
        }
        composeTestRule.onNodeWithContentDescription(label = "QR code").assertDoesNotExist()
    }

    @Test
    fun `test that QR code bitmap is shown when QR code is available`() = runTest {
        val uiState = MyCodeUIState.QRCodeAvailable(
            contactLink = "https://contact.link",
            avatarBgColor = Color.Red.toArgb(),
            avatarContent = TextAvatarContent("Jack")
        )
        prepareQRCodeMapper()

        composeTestRule.setContent {
            MyQRCodeView(
                uiState = uiState,
                onButtonClicked = { },
                onScroll = {},
                qrCodeMapper = qrCodeMapper,
            )
        }

        composeTestRule.onNodeWithTag(testTag = "QR Code Container").assertExists()
    }

    private suspend fun prepareQRCodeMapper() {
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        whenever(qrCodeMapper(any(), any(), any(), any(), any())).thenReturn(dummyBitmap)
    }
}
