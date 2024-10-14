package mega.privacy.android.app.presentation.transfers.starttransfer.view.filespermission

import mega.privacy.android.shared.resources.R as sharedResR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class FilesPermissionViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val onAllowClick = mock<() -> Unit>()
    private val onDoNotShowAgainClick = mock<() -> Unit>()
    private val onStartTransferAndDismiss = mock<() -> Unit>()

    @Test
    fun `test that FilesPermissionView is correctly displayed`() {
        initComposeRule()
        composeRule.apply {
            onNodeWithTag(FILES_PERMISSION_VIEW_TAG).assertIsDisplayed()
            onNodeWithTag(IMAGE_TAG).assertIsDisplayed()
            onNodeWithTag(TITLE_TAG).assertIsDisplayed()
            onNodeWithText(sharedResR.string.files_permission_screen_title).assertIsDisplayed()
            onNodeWithTag(MESSAGE_TAG).assertIsDisplayed()
            onNodeWithText(sharedResR.string.files_permission_screen_message).assertIsDisplayed()
            onNodeWithTag(ALLOW_BUTTON_TAG).assertIsDisplayed()
            onNodeWithText(R.string.dialog_positive_button_allow_permission).assertIsDisplayed()
            onNodeWithTag(NOT_NOW_BUTTON_TAG).assertIsDisplayed()
            onNodeWithText(R.string.permissions_not_now_button).assertIsDisplayed()
            onNodeWithTag(DO_NOT_SHOW_AGAIN_BUTTON_TAG).assertIsDisplayed()
            onNodeWithText(sharedResR.string.files_permission_screen_do_not_show_again_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that Allow permission button invokes correctly on click`() {
        initComposeRule()
        composeRule.onNodeWithTag(ALLOW_BUTTON_TAG).performClick()
        verify(onAllowClick).invoke()
    }

    @Test
    fun `test that Not now button invokes correctly on click`() {
        initComposeRule()
        composeRule.onNodeWithTag(NOT_NOW_BUTTON_TAG).performClick()
        verify(onStartTransferAndDismiss).invoke()
    }

    @Test
    fun `test that Do not show this again button invokes correctly on click`() {
        initComposeRule()
        composeRule.onNodeWithTag(DO_NOT_SHOW_AGAIN_BUTTON_TAG).performClick()
        verify(onDoNotShowAgainClick).invoke()
        verify(onStartTransferAndDismiss).invoke()
    }

    private fun initComposeRule() {
        composeRule.setContent {
            FilesPermissionView(
                onAllowClick = onAllowClick,
                onDoNotShowAgainClick = onDoNotShowAgainClick,
                onStartTransferAndDismiss = onStartTransferAndDismiss
            )
        }
    }
}