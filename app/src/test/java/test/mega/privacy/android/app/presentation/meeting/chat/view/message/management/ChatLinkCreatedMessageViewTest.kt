package test.mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatLinkCreatedView
import mega.privacy.android.app.utils.TextUtil
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatLinkCreatedMessageViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when chat link is created`() {
        val ownerActionFullName = "Owner"
        composeTestRule.setContent {
            ChatLinkCreatedView(
                ownerActionFullName = ownerActionFullName,
            )
        }
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.message_created_chat_link,
                    ownerActionFullName
                )
            )
        ).assertExists()
    }
}