package mega.privacy.android.core.ui.controls.text

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatUnreadCountTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that unread count is displayed correctly`() {
        val count = 5

        composeTestRule.setContent {
            ChatUnreadCountText(
                modifier = Modifier,
                count = count
            )
        }

        composeTestRule.onNodeWithTag("chat_unread_count:text").assertTextEquals(count.toString())
    }

    @Test
    fun `test that max count is displayed when count exceeds the maximum`() {
        val count = 110

        composeTestRule.setContent {
            ChatUnreadCountText(
                modifier = Modifier,
                count = count
            )
        }

        composeTestRule.onNodeWithTag("chat_unread_count:text").assertTextEquals("99")
    }
}
