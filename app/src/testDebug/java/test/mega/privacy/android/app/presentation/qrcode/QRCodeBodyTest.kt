package test.mega.privacy.android.app.presentation.qrcode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.qrcode.MyQRCodeHeader
import mega.privacy.android.app.presentation.qrcode.model.MyQRTab
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyQRCodeUIState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class QRCodeBodyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalPagerApi::class)
    @Test
    fun `test that share button is shown when QR code is valid`() = runTest {
        val uiState = MyQRCodeUIState(contactLink = "https://valid_contact_link")
        composeTestRule.setContent {
            val pagerState = rememberPagerState(
                initialPage = MyQRTab.MyQRCode.ordinal
            )

            MyQRCodeHeader(
                qrCodeUIState = uiState,
                showMoreMenu = true,
                pagerState = pagerState,
                onShowMoreClicked = {},
                onMenuDismissed = {},
                onSave = {},
                onGotoSettings = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onBackPressed = {},
                onShare = {},
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "ShareButton").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Test
    fun `test that share button is hidden when QR code is not available`() = runTest {
        val uiState = MyQRCodeUIState(contactLink = null)
        composeTestRule.setContent {
            val pagerState = rememberPagerState(
                initialPage = MyQRTab.MyQRCode.ordinal
            )

            MyQRCodeHeader(
                qrCodeUIState = uiState,
                showMoreMenu = true,
                pagerState = pagerState,
                onShowMoreClicked = {},
                onMenuDismissed = {},
                onSave = {},
                onGotoSettings = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onBackPressed = {},
                onShare = {},
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "ShareButton").assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Test
    fun `test that header buttons are hidden when scan code tab is active`() = runTest {
        val uiState = MyQRCodeUIState(contactLink = null)
        composeTestRule.setContent {
            val pagerState = rememberPagerState(
                initialPage = MyQRTab.ScanQRCode.ordinal
            )

            MyQRCodeHeader(
                qrCodeUIState = uiState,
                showMoreMenu = true,
                pagerState = pagerState,
                onShowMoreClicked = {},
                onMenuDismissed = {},
                onSave = {},
                onGotoSettings = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onBackPressed = {},
                onShare = {},
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "ShareButton").assertDoesNotExist()
            onNodeWithTag(testTag = "MoreButton").assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Test
    fun `test that drop down menu is shown showMoreMenu is true and QR code tab is active`() {
        val uiState = MyQRCodeUIState(contactLink = "https://valid_contact_link")
        composeTestRule.setContent {
            val pagerState = rememberPagerState(
                initialPage = MyQRTab.MyQRCode.ordinal
            )

            MyQRCodeHeader(
                qrCodeUIState = uiState,
                showMoreMenu = true,
                pagerState = pagerState,
                onShowMoreClicked = {},
                onMenuDismissed = {},
                onSave = {},
                onGotoSettings = {},
                onResetQRCode = {},
                onDeleteQRCode = {},
                onBackPressed = {},
                onShare = {},
            )
        }

        composeTestRule.run {
            onNodeWithTag(testTag = "DropDownMenu").assertIsDisplayed()
        }
    }
}
