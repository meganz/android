package test.mega.privacy.android.app.presentation.qrcode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.qrcode.DROPDOWN_TAG
import mega.privacy.android.app.presentation.qrcode.MORE_TAG
import mega.privacy.android.app.presentation.qrcode.QRCodeTopBar
import mega.privacy.android.app.presentation.qrcode.SHARE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class QRCodeTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(isQRCodeAvailable: Boolean, showMoreMenu: Boolean) {
        composeTestRule.setContent {
            QRCodeTopBar(
                context = mock(),
                isQRCodeAvailable = isQRCodeAvailable,
                showMoreMenu = showMoreMenu,
                onShowMoreClicked = { },
                onMenuDismissed = { },
                onSave = { },
                onResetQRCode = { },
                onDeleteQRCode = { },
                onBackPressed = { },
                onShare = { }
            )
        }
    }

    @Test
    fun `test that share and more button is shown when QR code is valid`() {
        setComposeContent(isQRCodeAvailable = true, showMoreMenu = false)
        composeTestRule.run {
            onNodeWithTag(testTag = SHARE_TAG).assertIsDisplayed()
            onNodeWithTag(testTag = MORE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that share and more button is not shown when QR code is not valid`() {
        setComposeContent(isQRCodeAvailable = false, showMoreMenu = false)
        composeTestRule.run {
            onNodeWithTag(testTag = SHARE_TAG).assertDoesNotExist()
            onNodeWithTag(testTag = MORE_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that drop down menu is shown when showMoreMenu is true`() {
        setComposeContent(isQRCodeAvailable = true, showMoreMenu = true)
        composeTestRule.run {
            onNodeWithTag(DROPDOWN_TAG).assertIsDisplayed()
        }
    }
}