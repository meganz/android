package test.mega.privacy.android.app.presentation.meeting.chat.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.getLastSeenString
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InvalidUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.normal.TextUiMessage
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.ChatAppBar
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.TEST_TAG_NOTIFICATION_MUTE
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.TEST_TAG_PRIVATE_ICON
import mega.privacy.android.app.presentation.meeting.chat.view.appbar.TEST_TAG_USER_CHAT_STATE
import mega.privacy.android.core.ui.controls.appbar.TEST_TAG_APP_BAR
import mega.privacy.android.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import test.mega.privacy.android.app.onNodeWithPlural
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ChatAppBarTest {
    private val actionPressed = mock<(ChatRoomMenuAction) -> Unit>()
    private val showGroupOrContactInfoActivity = mock<() -> Unit>()

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that title shows correctly when passing title to uiState`() {
        val title = "my chat room title"
        initComposeRuleContent(
            ChatUiState(chat = mock<ChatRoom> { on { this.title } doReturn title })
        )
        composeTestRule.onAllNodesWithText(title).onFirst().assertIsDisplayed()
    }

    @Test
    fun `test that status icon is not visible when the user chat status is null`() {
        initComposeRuleContent(
            ChatUiState(userChatStatus = null)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_USER_CHAT_STATE).assertDoesNotExist()
    }

    @Test
    fun `test that status icon is not visible when the user chat status is invalid`() {
        initComposeRuleContent(
            ChatUiState(userChatStatus = UserChatStatus.Invalid)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_USER_CHAT_STATE).assertDoesNotExist()
    }

    @Test
    fun `test that status icon is visible when the user chat status is online`() {
        initComposeRuleContent(
            ChatUiState(userChatStatus = UserChatStatus.Online)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_USER_CHAT_STATE, true).assertIsDisplayed()
    }


    @Test
    fun `test that mute icon is visible when chat notification is mute`() {
        initComposeRuleContent(
            ChatUiState(isChatNotificationMute = true)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_NOTIFICATION_MUTE, true).assertIsDisplayed()
    }

    @Test
    fun `test that mute icon is hidden when chat notification is mute`() {
        initComposeRuleContent(
            ChatUiState(isChatNotificationMute = false)
        )
        composeTestRule.onNodeWithTag(TEST_TAG_NOTIFICATION_MUTE).assertDoesNotExist()
    }

    @Test
    fun `test that private icon is visible when chat room is private`() {
        initComposeRuleContent(
            ChatUiState(chat = mock<ChatRoom> { on { isPublic } doReturn false })
        )
        composeTestRule.onNodeWithTag(TEST_TAG_PRIVATE_ICON, true).assertIsDisplayed()
    }

    @Test
    fun `test that private icon is hidden when chat room is public`() {
        initComposeRuleContent(
            ChatUiState(chat = mock<ChatRoom> { on { isPublic } doReturn true })
        )
        composeTestRule.onNodeWithTag(TEST_TAG_PRIVATE_ICON).assertDoesNotExist()
    }


    @Test
    fun `test that audio call is hidden when my permission is unknown`() {
        initComposeRuleContent(ChatUiState())
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when my permission is removed`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Removed }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when my permission is read only`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when is joining`() {
        initComposeRuleContent(ChatUiState(isJoining = true))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that audio call is hidden when is preview mode`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isPreview } doReturn true }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that audio call is shown and enabled when my permission is standard`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Standard },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that audio call is shown and enabled when my permission is moderator`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Moderator },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that audio call is shown but disabled when my permission is moderator and this chat has a call`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Moderator },
                callInThisChat = mock(),
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION, true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        verifyNoInteractions(actionPressed)
    }

    @Test
    fun `test that video call is hidden when my permission is unknown`() {
        initComposeRuleContent(ChatUiState())
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that video call is hidden when my permission is removed`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Removed }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that video call is hidden when my permission is read only`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that video call is hidden when is joining`() {
        initComposeRuleContent(ChatUiState(isJoining = true))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that video call is hidden when is preview mode`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isPreview } doReturn true }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that video call is hidden when the chat is a group`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isGroup } doReturn true }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that video call is shown and enabled when my permission is standard`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Standard },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that video call is shown and enabled when my permission is moderator`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Moderator },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION, true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that video call is shown but disabled when my permission is moderator and this chat has a call`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Moderator },
                callInThisChat = mock(),
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_VIDEO_CALL_ACTION, true)
            .apply {
                assertIsDisplayed()
                performClick()
            }
        verifyNoInteractions(actionPressed)
    }

    @Test
    fun `test that add participants is available when the chat is a group my permission is moderator`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn true
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ADD_PARTICIPANTS_ACTION)
            .assertExists()
    }

    @Test
    fun `test that add participants is available when the chat is a group and is open invite`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isActive } doReturn true
                    on { isGroup } doReturn true
                    on { isOpenInvite } doReturn true
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ADD_PARTICIPANTS_ACTION)
            .assertIsDisplayed()
    }

    @Test
    fun `test that add participants is not available when the chat is not a group`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isGroup } doReturn false }))
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ADD_PARTICIPANTS_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that add participants is not available when is joining`() {
        initComposeRuleContent(ChatUiState(isJoining = true))
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ADD_PARTICIPANTS_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that add participants is not available when the group is not active`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isActive } doReturn false
                },
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ADD_PARTICIPANTS_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that add participants is not available when the group does not open invite`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isOpenInvite } doReturn false
                },
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that add participants is not available when my permission is not moderator`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Standard
                    on { isGroup } doReturn true
                }
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ADD_PARTICIPANTS_ACTION)
            .assertDoesNotExist()
    }


    @Test
    fun `test that archive label shown when chat is archived`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isArchived } doReturn true },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.archived_chat))
            .assertIsDisplayed()
    }

    @Test
    fun `test that archive label doesn't show when chat is not archived`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isArchived } doReturn false },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.archived_chat))
            .assertDoesNotExist()
    }

    @Test
    fun `test that read only label shown when in a chat I have read only permission`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.observer_permission_label_participants_panel))
            .assertIsDisplayed()
    }

    @Test
    fun `test that ready only label does not show in a chat I do not have read only permission`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.observer_permission_label_participants_panel))
            .assertDoesNotExist()
    }

    @Test
    fun `test that inactive label shown when in a chat I have read only permission`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Removed },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.inactive_chat))
            .assertIsDisplayed()
    }

    @Test
    fun `test that inactive label does not show in a chat I do not have read only permission`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.inactive_chat))
            .assertDoesNotExist()
    }


    @Test
    fun `test that Info menu action is not available when chat is joining or leaving`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isOpenInvite } doReturn false
                },
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_INFO_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Info menu action is not available when chat is in preview mode`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isPreview } doReturn true }))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_INFO_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Info menu action is not available when chat is not connected`() {
        initComposeRuleContent(ChatUiState(isConnected = false))
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_INFO_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Info menu action is not available when 1on1 chat is read only`() {
        initComposeRuleContent(
            ChatUiState(chat = mock<ChatRoom> {
                on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly
                on { isGroup } doReturn false
            }
            )
        )

        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_INFO_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Info menu action is available in 1on1 chat and my permission is Moderator`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn false
                    on { isPreview } doReturn false
                },
                isJoining = false,
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_INFO_ACTION).assertIsDisplayed()
    }


    @Test
    fun `test that Info menu action is available in group chat`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isPreview } doReturn false
                },
                isJoining = false,
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_INFO_ACTION).assertIsDisplayed()
    }


    fun `test that online label shows if the chat is 1to1 and the contacts status is online`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                userChatStatus = UserChatStatus.Online
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.online_status))
            .assertIsDisplayed()
    }

    @Test
    fun `test that away label shows if the chat is 1to1 and the contacts status is away`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                isConnected = true,
                userChatStatus = UserChatStatus.Away
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.away_status))
            .assertIsDisplayed()
    }

    @Test
    fun `test that busy label shows if the chat is 1to1 and the contacts status is busy`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                isConnected = true,
                userChatStatus = UserChatStatus.Busy
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.busy_status))
            .assertIsDisplayed()
    }

    @Test
    fun `test that offline label shows if the chat is 1to1 and the contacts status is offline`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                isConnected = true,
                userChatStatus = UserChatStatus.Offline
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.offline_status))
            .assertIsDisplayed()
    }

    @Test
    fun `test that last green label shows if the chat is 1to1 and the contacts last green is not null`() {
        val lastGreen = initComposeRuleContentWithLastGreen(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                isConnected = true,
                userLastGreen = 123456
            )
        )
        composeTestRule.onNodeWithText(lastGreen).assertIsDisplayed()
    }

    @Test
    fun `test that number of participants shows if the chat is a group and does not have a custom subtitle`() {
        val count = 5
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                customSubtitleList = null,
                participantsCount = count.toLong()
            )
        )
        composeTestRule.onNodeWithPlural(R.plurals.subtitle_of_group_chat, count)
            .assertExists()
    }

    @Test
    fun `test that number of participants shows if the chat is in preview mode`() {
        val count = 5
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isPreview } doReturn true },
                isConnected = true,
                participantsCount = count.toLong()
            )
        )
        composeTestRule.onNodeWithPlural(R.plurals.subtitle_of_group_chat, count - 1)
            .assertExists()
    }


    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle and has only me as participant`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                customSubtitleList = emptyList(),
            )
        )
        composeTestRule.onNodeWithText(R.string.bucket_word_me).assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle, is preview and does not have participants`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn false
                    on { isPreview } doReturn true
                },
                isConnected = true,
                customSubtitleList = emptyList(),
            )
        )
        composeTestRule.onNodeWithPlural(R.plurals.subtitle_of_group_chat, 0)
            .assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle and has only one participant apart from me`() {
        val userA = "A"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                customSubtitleList = listOf(userA),
            )
        )
        val me = composeTestRule.activity.getString(R.string.bucket_word_me)
        composeTestRule.onNodeWithText("$userA, $me").assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle, is preview and has only one participant`() {
        val userA = "A"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isPreview } doReturn true
                },
                isConnected = true,
                customSubtitleList = listOf(userA),
            )
        )
        composeTestRule.onNodeWithText(userA).assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle and has two participants apart from me`() {
        val userA = "A"
        val userB = "B"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                customSubtitleList = listOf(userA, userB),
            )
        )
        val me = composeTestRule.activity.getString(R.string.bucket_word_me)
        composeTestRule.onNodeWithText("$userA, $userB, $me").assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle, is preview and has two participants`() {
        val userA = "A"
        val userB = "B"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isPreview } doReturn true
                },
                isConnected = true,
                customSubtitleList = listOf(userA, userB),
            )
        )
        composeTestRule.onNodeWithText("$userA, $userB").assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle and has three participants apart from me`() {
        val userA = "A"
        val userB = "B"
        val userC = "C"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                customSubtitleList = listOf(userA, userB, userC),
            )
        )
        val me = composeTestRule.activity.getString(R.string.bucket_word_me)
        composeTestRule.onNodeWithText("$userA, $userB, $userC, $me").assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle, is preview and has three participants`() {
        val userA = "A"
        val userB = "B"
        val userC = "C"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isPreview } doReturn true
                },
                isConnected = true,
                customSubtitleList = listOf(userA, userB, userC),
            )
        )
        composeTestRule.onNodeWithText("$userA, $userB, $userC").assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle and has five participants apart from me`() {
        val userA = "A"
        val userB = "B"
        val userC = "C"
        val more = "3"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
                customSubtitleList = listOf(userA, userB, userC, more),
            )
        )
        val customSubtitle = composeTestRule.activity.getString(
            R.string.custom_subtitle_of_group_chat,
            "$userA, $userB, $userC",
            more.toInt()
        )
        composeTestRule.onNodeWithText(customSubtitle).assertExists()
    }

    @Test
    fun `test that custom subtitle shows if the chat is a group, has custom subtitle, is preview and has five participants`() {
        val userA = "A"
        val userB = "B"
        val userC = "C"
        val more = "2"
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isGroup } doReturn true
                    on { isPreview } doReturn true
                },
                isConnected = true,
                customSubtitleList = listOf(userA, userB, userC, more),
            )
        )
        val customSubtitle = composeTestRule.activity.getString(
            R.string.custom_subtitle_of_group_chat,
            "$userA, $userB, $userC",
            more.toInt()
        )
        composeTestRule.onNodeWithText(customSubtitle).assertExists()
    }

    @Test
    fun `test that Reconnecting to chat label is shown when chat has no connection`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = false
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.invalid_connection_state))
            .assertExists()
    }

    @Test
    fun `test that Reconnecting to chat label is not shown when chat has connection`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = true
            )
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.invalid_connection_state))
            .assertDoesNotExist()
    }

    @Test
    fun `test that audio call is shown but disabled when chat is a waiting room and I am not a morerator`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Standard
                    on { isWaitingRoom } doReturn true
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION, true).apply {
            assertIsDisplayed()
            performClick()
        }
        verifyNoInteractions(actionPressed)
    }

    @Test
    fun `test that toolbar tap is disabled if is joining`() {
        initComposeRuleContent(ChatUiState(isJoining = true))
        composeTestRule.onNodeWithTag(TEST_TAG_APP_BAR).apply {
            assertIsDisplayed()
            performClick()
        }
        verifyNoInteractions(showGroupOrContactInfoActivity)
    }

    @Test
    fun `test that toolbar tap is disabled if is preview mode`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isPreview } doReturn true },
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_APP_BAR).apply {
            assertIsDisplayed()
            performClick()
        }
        verifyNoInteractions(showGroupOrContactInfoActivity)
    }

    @Test
    fun `test that toolbar tap is disabled if is not connected`() {
        initComposeRuleContent(
            ChatUiState(
                isConnected = false
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_APP_BAR).apply {
            assertIsDisplayed()
            performClick()
        }
        verifyNoInteractions(showGroupOrContactInfoActivity)
    }

    @Test
    fun `test that toolbar tap is disabled if not a group and my permission is read only`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly
                    on { isGroup } doReturn false
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_APP_BAR).apply {
            assertIsDisplayed()
            performClick()
        }
        verifyNoInteractions(showGroupOrContactInfoActivity)
    }

    @Test
    fun `test that toolbar tap is enabled if is a group`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn true },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_APP_BAR).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(showGroupOrContactInfoActivity).invoke()
    }

    @Test
    fun `test that toolbar tap is enabled if is not a group and I have moderator permission`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn false
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_APP_BAR).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(showGroupOrContactInfoActivity).invoke()
    }

    @Test
    fun `test that unmute menu action is shown when a group chat meets the conditions`() {
        initComposeRuleContent(
            uiStateToShowUnmuteInGroupChat()
        )

        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION).assertExists()
    }

    @Test
    fun `test that unmute menu action is shown when a non-group chat meets the conditions`() {
        initComposeRuleContent(
            uiStateToShowUnmuteIn1on1Chat()
        )

        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION).assertExists()
    }

    private fun uiStateToShowUnmuteIn1on1Chat() =
        ChatUiState(
            chat = mock<ChatRoom> {
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                on { isGroup } doReturn false
                on { isPreview } doReturn false
            },
            isJoining = false,
            isChatNotificationMute = true,
            isConnected = true,
        )

    private fun uiStateToShowUnmuteInGroupChat(): ChatUiState =
        ChatUiState(
            chat = mock<ChatRoom> {
                on { isGroup } doReturn true
                on { isPreview } doReturn false
                on { isActive } doReturn true
            },
            isJoining = false,
            isChatNotificationMute = true,
            isConnected = true,
        )

    @Test
    fun `test that unmute menu action is not shown when it is joining`() {
        initComposeRuleContent(
            uiStateToShowUnmuteIn1on1Chat().copy(isJoining = true)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that unmute menu action is not shown when mute notification is set to true`() {
        initComposeRuleContent(
            uiStateToShowUnmuteIn1on1Chat().copy(isChatNotificationMute = true)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that unmute menu action is not shown when chat is in preview mode`() {
        initComposeRuleContent(
            uiStateToShowUnmuteIn1on1Chat().copy(chat = mock<ChatRoom> { on { isPreview } doReturn true })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that unmute menu action is not shown when chat is disconnected`() {
        initComposeRuleContent(
            uiStateToShowUnmuteIn1on1Chat().copy(isConnected = false)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that unmute menu action is not shown when it is a group chat but not active`() {
        initComposeRuleContent(
            uiStateToShowUnmuteInGroupChat().copy(chat = mock<ChatRoom> { on { isActive } doReturn false })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that unmute menu action is not shown when it is a 1on1 chat but I am not a moderator`() {
        initComposeRuleContent(
            uiStateToShowUnmuteIn1on1Chat().copy(chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mute menu action is shown when a group chat meets the conditions`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat()
        )

        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }

        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_MUTE_ACTION).assertExists()
    }

    @Test
    fun `test that mute menu action is shown when a non-group chat meets the conditions`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat()
        )

        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_MUTE_ACTION).assertIsDisplayed()
    }

    private fun uiStateToShowMuteIn1on1Chat() =
        ChatUiState(
            chat = mock<ChatRoom> {
                on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                on { isGroup } doReturn false
                on { isPreview } doReturn false
            },
            isJoining = false,
            isChatNotificationMute = false,
            isConnected = true,
        )

    private fun uiStateToShowMuteInGroupChat(): ChatUiState =
        ChatUiState(
            chat = mock<ChatRoom> {
                on { isGroup } doReturn true
                on { isPreview } doReturn false
                on { isActive } doReturn true
            },
            isJoining = false,
            isChatNotificationMute = true,
            isConnected = true,
        )

    @Test
    fun `test that mute menu action is not shown when it is joining`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat().copy(isJoining = true)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mute menu action is not shown when mute notification is set to true`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat().copy(isChatNotificationMute = true)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mute menu action is not shown when chat is in preview mode`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat().copy(chat = mock<ChatRoom> { on { isPreview } doReturn true })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mute menu action is not shown when chat is disconnected`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat().copy(isConnected = false)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mute menu action is not shown when it is a group chat but not active`() {
        initComposeRuleContent(
            uiStateToShowMuteInGroupChat().copy(chat = mock<ChatRoom> { on { isActive } doReturn false })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that mute menu action is not shown when it is a 1on1 chat but I am not a moderator`() {
        initComposeRuleContent(
            uiStateToShowMuteIn1on1Chat().copy(chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNMUTE_ACTION)
            .assertDoesNotExist()
    }


    @Test
    fun `test that Clear menu action is not available when is not connected`() {
        initComposeRuleContent(
            ChatUiState(isConnected = false)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Clear menu action is not available when is preview mode`() {
        initComposeRuleContent(
            ChatUiState(chat = mock<ChatRoom> { on { isPreview } doReturn true })
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Clear menu action is not available when is joining`() {
        initComposeRuleContent(
            ChatUiState(isJoining = true)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Clear menu action is not available in group chat with standard permissions`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Standard
                    on { isGroup } doReturn true
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Clear menu action is not available in 1on1 chat with read only permissions`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.ReadOnly
                    on { isGroup } doReturn false
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertDoesNotExist()
    }

    fun `test that Clear menu action is available in group chat with moderator permissions`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn true
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that Clear menu action is available in 1on1 chat with standard permissions`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Standard
                    on { isGroup } doReturn false
                },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertIsDisplayed()
    }

    @Test
    fun `test that end call for all is available when the chat is a group my permission is moderator and has a call in this chat`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn true
                },
                callInThisChat = mock(),
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_END_CALL_FOR_ALL_ACTION)
            .assertExists()
    }

    @Test
    fun `test that end call for all is not available when the chat is a group my permission is moderator and hasn't a call in this chat`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn true
                },
                callInThisChat = null,
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_END_CALL_FOR_ALL_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Archive menu action is not available if is joining`() {
        initComposeRuleContent(
            ChatUiState(isJoining = true)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Archive menu action is not available if is preview mode`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isPreview } doReturn true }))
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Archive menu action is not available if not connected`() {
        initComposeRuleContent(
            ChatUiState(isConnected = false)
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Archive menu action is not available if already archived`() {
        initComposeRuleContent(ChatUiState(chat = mock<ChatRoom> { on { isArchived } doReturn true }))
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_CLEAR_ACTION).assertDoesNotExist()
    }

    @Test
    fun `test that Archive menu action is available if is not archived`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isArchived } doReturn false },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_ARCHIVE_ACTION)
            .assertIsDisplayed()
    }

    @Ignore("Need to implement a new solution that works with paged loading")
    @Test
    fun `test that select menu action is available if messages contains text message`() {
        val textMessage = mock<TextUiMessage> {
            on { message }.thenReturn(mock())
        }
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isPreview } doReturn false },
                messages = listOf(textMessage),
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_SELECT_ACTION)
            .assertIsDisplayed()
    }

    @Test
    fun `test that select menu action is not available if messages contains chat invalid message`() {
        val invalidMessage = mock<InvalidUiMessage.UnrecognizableInvalidUiMessage>()
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { isPreview } doReturn false
                    on { isArchived } doReturn false
                },
                isConnected = true,
                messages = listOf(invalidMessage),
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_SELECT_ACTION)
            .assertDoesNotExist()
    }

    @Test
    fun `test that Unarchive menu action is available if it is archived`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isArchived } doReturn true },
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(ChatRoomMenuAction.TEST_TAG_UNARCHIVE_ACTION)
            .assertIsDisplayed()
    }

    private fun initComposeRuleContent(state: ChatUiState) {
        composeTestRule.setContent {
            ChatAppBar(
                uiState = state,
                onBackPressed = {},
                onMenuActionPressed = actionPressed,
                showGroupOrContactInfoActivity = showGroupOrContactInfoActivity
            )
        }
    }

    private fun initComposeRuleContentWithLastGreen(state: ChatUiState): String {
        var lastGreen = "last green"
        composeTestRule.setContent {
            ChatView(
                uiState = state,
                onBackPressed = {},
                onMenuActionPressed = actionPressed,
                messageListView = { _, _, _, _ -> },
            )
            lastGreen = getLastSeenString(lastGreen = state.userLastGreen) ?: ""
        }
        return lastGreen
    }
}