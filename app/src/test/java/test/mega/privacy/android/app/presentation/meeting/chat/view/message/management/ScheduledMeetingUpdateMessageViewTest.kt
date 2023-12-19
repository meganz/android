package test.mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.meetingupdate.ScheduledMeetingUpdateMessageView
import mega.privacy.android.app.utils.TextUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduledMeetingUpdateMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when meeting updated`() {
        val ownerActionFullName = "Owner"
        composeTestRule.setContent {
            ScheduledMeetingUpdateMessageView(
                ownerActionFullName = ownerActionFullName,
            )
        }
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.chat_chat_room_message_updated_scheduled_meeting,
                    ownerActionFullName,
                )
            )
        ).assertExists()
    }
}