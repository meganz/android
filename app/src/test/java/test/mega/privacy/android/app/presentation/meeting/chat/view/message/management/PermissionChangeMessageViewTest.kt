package test.mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.permission.PermissionChangeMessageView
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionChangeMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when permission changed to ChatRoomPermission Moderator`() {
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
                    R.string.chat_chat_room_message_permissions_changed_to_host,
                    targetActionFullName,
                    ownerActionFullName,
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that text shows correctly when permission changed to ChatRoomPermission Standard`() {
        val ownerActionFullName = "Owner1"
        val targetActionFullName = "Target1"
        initComposeRuleContent(
            privilege = ChatRoomPermission.Standard,
            ownerActionFullName = ownerActionFullName,
            targetActionFullName = targetActionFullName,
            userHandle = 123456780L,
            handleOfAction = 1234567891L,
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.chat_chat_room_message_permissions_changed_to_standard,
                    targetActionFullName,
                    ownerActionFullName,
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that text shows correctly when permission changed to ChatRoomPermission ReadOnly`() {
        val ownerActionFullName = "Owner2"
        val targetActionFullName = "Target2"
        initComposeRuleContent(
            privilege = ChatRoomPermission.ReadOnly,
            ownerActionFullName = ownerActionFullName,
            targetActionFullName = targetActionFullName,
            userHandle = 123456781L,
            handleOfAction = 1234567892L,
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.chat_chat_room_message_permissions_changed_to_read_only,
                    targetActionFullName,
                    ownerActionFullName,
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
            PermissionChangeMessageView(
                message = PermissionChangeMessage(
                    privilege = privilege,
                    userHandle = userHandle,
                    handleOfAction = handleOfAction,
                    isMine = false,
                    msgId = 1234567890L,
                    time = System.currentTimeMillis(),
                ),
                ownerActionFullName = ownerActionFullName,
                targetActionFullName = targetActionFullName,
            )
        }
    }
}