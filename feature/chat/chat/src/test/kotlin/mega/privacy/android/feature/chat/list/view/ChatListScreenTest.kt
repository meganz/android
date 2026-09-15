package mega.privacy.android.feature.chat.list.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.feature.chat.list.model.ChatListTabState
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem
import mega.privacy.android.feature.chat.list.model.MeetingAction
import mega.privacy.android.navigation.contract.state.LocalTopLevelNavKeyClass
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val navigationIconContentDescription = "Navigation Icon"

    private val meetingsTabLabel: String
        get() = composeRule.activity.getString(sharedR.string.chat_tab_meetings_title)

    private object OtherSectionNavKey : NavKey

    private fun setupComposeContent(topLevelNavKeyClass: KClass<out NavKey>?) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides topLevelNavKeyClass) {
                AndroidThemeForPreviews {
                    TestChatListScreen()
                }
            }
        }
    }

    @Composable
    private fun TestChatListScreen() {
        ChatListScreen(
            uiState = ChatListUiState.Loading,
            showMeetingTab = false,
            onBackClick = {},
            onItemClick = {},
            onNewChatClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onSearchQueryChange = {},
        )
    }

    @Test
    fun `test that no back button is shown when the chat list is the navigation bar destination`() {
        setupComposeContent(topLevelNavKeyClass = ChatListNavKey::class)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun `test that a back button is shown when another section is the navigation bar destination`() {
        setupComposeContent(topLevelNavKeyClass = OtherSectionNavKey::class)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun `test that a back button is shown when hosted outside the main navigation display`() {
        setupComposeContent(topLevelNavKeyClass = null)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the new chat fab is shown when the Chats tab is selected`() {
        setupPopulatedScreen(showMeetingTab = false)

        composeRule.onNodeWithTag(CHAT_LIST_FAB_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the meeting fab is shown when the Meetings tab is selected`() {
        setupPopulatedScreen(showMeetingTab = true)

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_LIST_FAB_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the meeting actions sheet is shown when the meeting fab is clicked`() {
        setupPopulatedScreen(showMeetingTab = true)

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).performClick()

        composeRule.onNodeWithTag(MEETING_ACTIONS_START_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MEETING_ACTIONS_JOIN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MEETING_ACTIONS_SCHEDULE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that picking schedule meeting reports the Schedule action`() {
        val pickedActions = mutableListOf<MeetingAction>()
        setupPopulatedScreen(showMeetingTab = true, onMeetingActionClick = pickedActions::add)

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).performClick()
        composeRule.onNodeWithTag(MEETING_ACTIONS_SCHEDULE_TAG).performClick()
        composeRule.waitForIdle()

        assertThat(pickedActions).containsExactly(MeetingAction.Schedule)
    }

    @Test
    fun `test that the selected tab and its fab survive state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { RestorableChatListScreen() }

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_LIST_FAB_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that a tab switch survives state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { RestorableChatListScreen(showMeetingTab = false) }

        composeRule.onNodeWithTag(CHAT_LIST_FAB_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(meetingsTabLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_LIST_FAB_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the meeting actions sheet survives state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { RestorableChatListScreen() }

        composeRule.onNodeWithTag(CHAT_LIST_MEETING_FAB_TAG).performClick()
        composeRule.onNodeWithTag(MEETING_ACTIONS_SCHEDULE_TAG).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(MEETING_ACTIONS_START_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MEETING_ACTIONS_JOIN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MEETING_ACTIONS_SCHEDULE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the meeting blocked dialog survives state restoration`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            AndroidThemeForPreviews {
                var message by rememberSaveable { mutableStateOf<Int?>(null) }
                Column {
                    MegaText(
                        text = "show",
                        textColor = TextColor.Primary,
                        modifier = Modifier
                            .testTag("show_blocked_dialog")
                            .clickable {
                                message = sharedR.string.chat_start_meeting_already_in_call_error
                            },
                    )
                    message?.let {
                        MeetingBlockedByCallDialog(message = it, onDismiss = { message = null })
                    }
                }
            }
        }

        composeRule.onNodeWithTag("show_blocked_dialog").performClick()
        composeRule.onNodeWithTag(MEETING_BLOCKED_BY_CALL_DIALOG_TAG).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(MEETING_BLOCKED_BY_CALL_DIALOG_TAG).assertIsDisplayed()
    }

    @Composable
    private fun RestorableChatListScreen(showMeetingTab: Boolean = true) {
        CompositionLocalProvider(LocalTopLevelNavKeyClass provides ChatListNavKey::class) {
            AndroidThemeForPreviews {
                ChatListScreen(
                    uiState = populatedState(),
                    showMeetingTab = showMeetingTab,
                    onBackClick = {},
                    onItemClick = {},
                    onNewChatClick = {},
                    onMeetingActionClick = {},
                    onArchivedClick = {},
                    onSearchQueryChange = {},
                )
            }
        }
    }

    private fun setupPopulatedScreen(
        showMeetingTab: Boolean,
        onMeetingActionClick: (MeetingAction) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides ChatListNavKey::class) {
                AndroidThemeForPreviews {
                    ChatListScreen(
                        uiState = populatedState(),
                        showMeetingTab = showMeetingTab,
                        onBackClick = {},
                        onItemClick = {},
                        onNewChatClick = {},
                        onMeetingActionClick = onMeetingActionClick,
                        onArchivedClick = {},
                        onSearchQueryChange = {},
                    )
                }
            }
        }
    }

    private fun populatedState() = ChatListUiState.Data(
        chats = ChatListTabState.Results(persistentListOf(chatRoomUiItem(1L, "Chat"))),
        meetings = ChatListTabState.Results(persistentListOf(chatRoomUiItem(2L, "Meeting"))),
        chatsHaveUnread = false,
        meetingsHaveUnread = false,
    )

    private fun chatRoomUiItem(chatId: Long, title: String) = ChatRoomUiItem(
        chatId = chatId,
        title = title,
        lastMessage = null,
        lastTimestampFormatted = "",
        header = null,
        pendingMeeting = null,
        showRecurringIcon = false,
        unreadCount = 0,
        isMuted = false,
        highlight = false,
        isNoteToSelf = false,
        avatar = ChatRoomUiItem.ChatRoomUiAvatar.Group,
        status = ContactItemStatus.Unknown,
        hasOngoingCall = false,
    )
}
