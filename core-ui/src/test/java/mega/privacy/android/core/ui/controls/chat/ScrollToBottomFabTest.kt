package mega.privacy.android.core.ui.controls.chat

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
class ScrollToBottomFabTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that ScrollToBottomFab shows correctly`() {
        composeRule.setContent {
            ScrollToBottomFab(
                unreadCount = 0,
                onClick = {}
            )
        }
        composeRule.onNodeWithTag(SCROLL_TO_BOTTOM_FAB_TEST_TAG).assertExists()
    }

    @Test
    fun `test that unread count text shows correctly when unread count smaller than 99`() {
        val unreadCount = 5
        composeRule.setContent {
            ScrollToBottomFab(
                unreadCount = unreadCount,
                onClick = {}
            )
        }
        composeRule.onNodeWithText(unreadCount.toString()).assertExists()
    }

    @Test
    fun `test that unread count text shows correctly when unread count is more than 99`() {
        val unreadCount = 105
        composeRule.setContent {
            ScrollToBottomFab(
                unreadCount = unreadCount,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("+99").assertExists()
    }

    @Test
    fun `test that onClick is invoked when fab is clicked`() {
        val onClick: () -> Unit = mock()
        composeRule.setContent {
            ScrollToBottomFab(
                unreadCount = 0,
                onClick = onClick
            )
        }
        composeRule.onNodeWithTag(SCROLL_TO_BOTTOM_FAB_TEST_TAG).performClick()
        verify(onClick).invoke()
    }
}