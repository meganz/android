package mega.privacy.android.shared.original.core.ui.controls.banners

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class InlineBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that inline warning banner components are showing`() {
        val message = "To keep uploading data, upgrade to get more storage."
        val title = "Your storage is almost full"
        val actionButtonText = "Upgrade now"
        val onActionClick = mock<() -> Unit>()
        val onCloseClick = mock<() -> Unit>()
        composeTestRule.setContent {
            InlineWarningBanner(
                title = title,
                message = message,
                actionButtonText = actionButtonText,
                onActionButtonClick = onActionClick,
                onCloseClick = onCloseClick,
                modifier = Modifier,
            )
        }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INLINE_BANNER_HINT_ICON_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INLINE_BANNER_CLOSE_TEST_TAG).assertIsDisplayed()
            .performClick()
        verify(onCloseClick).invoke()
        composeTestRule.onNodeWithText(actionButtonText).assertIsDisplayed().performClick()
        verify(onActionClick).invoke()

    }

    @Test
    fun `test that inline error banner components are showing`() {
        val message = "To keep uploading data, upgrade to get more storage."
        val title = "Your storage is almost full"
        val actionButtonText = "Upgrade now"
        val onActionClick = mock<() -> Unit>()
        composeTestRule.setContent {
            InlineErrorBanner(
                title = title,
                message = message,
                actionButtonText = actionButtonText,
                onActionButtonClick = onActionClick,
                modifier = Modifier,
            )
        }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INLINE_BANNER_HINT_ICON_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INLINE_BANNER_CLOSE_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(actionButtonText).assertIsDisplayed().performClick()
        verify(onActionClick).invoke()

    }
}