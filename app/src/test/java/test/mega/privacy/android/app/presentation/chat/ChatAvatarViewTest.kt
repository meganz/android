package test.mega.privacy.android.app.presentation.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore("Ignore the unstable test. Will add the tests back once stability issue is resolved.")
class ChatAvatarViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that no avatar is shown`() {
        composeRule.setContent {
            ChatAvatarView(avatars = null)
        }

        composeRule.onNodeWithText("").assertExists()
    }

    @Test
    fun `test that a single avatar is shown`() {
        val avatar = ChatAvatarItem(placeholderText = "A", uri = "uri", color = 0xFFFFFFFF.toInt())

        composeRule.setContent {
            ChatAvatarView(avatars = listOf(avatar))
        }

        composeRule.onNodeWithText("A").assertExists()
    }

    @Test
    fun `test that multiple avatars are shown`() {
        val avatar1 =
            ChatAvatarItem(placeholderText = "A", uri = "uri1", color = 0xFFFFFFFF.toInt())
        val avatar2 =
            ChatAvatarItem(placeholderText = "B", uri = "uri2", color = 0xFFFFFFFF.toInt())

        composeRule.setContent {
            ChatAvatarView(avatars = listOf(avatar1, avatar2))
        }

        composeRule.onNodeWithText("A").assertExists()
        composeRule.onNodeWithText("B").assertExists()
    }
}
