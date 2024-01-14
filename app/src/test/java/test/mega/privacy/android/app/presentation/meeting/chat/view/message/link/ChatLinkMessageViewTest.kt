package test.mega.privacy.android.app.presentation.meeting.chat.view.message.link

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatGroupLinkContent
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatLinkMessageView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatLinkMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when link is valid`() {
        initComposeRuleContent(
            linkContent = ChatGroupLinkContent(
                numberOfParticipants = 10,
                name = "Group name",
                link = "https://mega.nz/chat/1234567890#1234567890",
            )
        )
        composeTestRule.onNodeWithText("Group name").assertExists()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(
                R.string.number_of_participants,
                10
            )
        ).assertExists()
        composeTestRule.onNodeWithText(Uri.parse("https://mega.nz/chat/1234567890#1234567890").authority.orEmpty())
            .assertExists()
    }

    @Test
    fun `test that text shows correctly when link is invalid`() {
        initComposeRuleContent(
            linkContent = ChatGroupLinkContent(
                numberOfParticipants = 0,
                name = "",
                link = "https://mega.nz/chat/1234567890#1234567890",
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.invalid_chat_link))
            .assertExists()
        composeTestRule.onNodeWithText(Uri.parse("https://mega.nz/chat/1234567890#1234567890").authority.orEmpty())
            .assertExists()
    }

    private fun initComposeRuleContent(linkContent: ChatGroupLinkContent) {
        composeTestRule.setContent {
            ChatLinkMessageView(
                linkContent = linkContent
            )
        }
    }
}