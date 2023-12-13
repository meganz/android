package test.mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.AlterParticipantsMessageView
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlterParticipantsMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when users added by themselves`() {
        val ownerActionFullName = "Owner"
        val targetActionFullName = "Target"
        initComposeRuleContent(
            privilege = ChatRoomPermission.Moderator,
            ownerActionFullName = ownerActionFullName,
            targetActionFullName = targetActionFullName,
            userHandle = 1234567890L,
            handleOfAction = 1234567890L,
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.message_joined_public_chat_autoinvitation,
                    ownerActionFullName
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that text shows correctly when user added by others`() {
        val ownerActionFullName = "Owner1"
        val targetActionFullName = "Target1"
        initComposeRuleContent(
            privilege = ChatRoomPermission.Moderator,
            ownerActionFullName = ownerActionFullName,
            targetActionFullName = targetActionFullName,
            userHandle = 123456780L,
            handleOfAction = 1234567891L,
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.message_add_participant,
                    targetActionFullName,
                    ownerActionFullName
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that text shows correctly when user left`() {
        val ownerActionFullName = "Owner2"
        val targetActionFullName = "Target2"
        initComposeRuleContent(
            privilege = ChatRoomPermission.Removed,
            ownerActionFullName = ownerActionFullName,
            targetActionFullName = targetActionFullName,
            userHandle = 1234567890L,
            handleOfAction = 1234567890L,
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.message_participant_left_group_chat,
                    ownerActionFullName
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that text shows correctly when user removed by others`() {
        val ownerActionFullName = "Owner3"
        val targetActionFullName = "Target3"
        initComposeRuleContent(
            privilege = ChatRoomPermission.Removed,
            ownerActionFullName = ownerActionFullName,
            targetActionFullName = targetActionFullName,
            userHandle = 1234567890L,
            handleOfAction = 1234567891L,
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.message_remove_participant,
                    targetActionFullName,
                    ownerActionFullName
                )
            )
        ).assertExists()
    }

    private fun initComposeRuleContent(
        privilege: ChatRoomPermission,
        ownerActionFullName: String,
        targetActionFullName: String,
        userHandle: Long,
        handleOfAction: Long,
    ) {
        composeTestRule.setContent {
            AlterParticipantsMessageView(
                message = AlterParticipantsMessage(
                    msgId = 123L,
                    time = System.currentTimeMillis(),
                    isMine = true,
                    userHandle = userHandle,
                    privilege = privilege,
                    handleOfAction = handleOfAction
                ),
                ownerActionFullName = ownerActionFullName,
                targetActionFullName = targetActionFullName,
            )
        }
    }
}