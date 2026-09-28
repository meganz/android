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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollToIndex
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
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.menu.TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.feature.chat.list.menu.CHAT_ROOM_ACTIONS_ARCHIVE_TAG
import mega.privacy.android.feature.chat.list.model.ChatRoomActionUiItem
import mega.privacy.android.icon.pack.IconPack
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
            onInviteFriendClick = {},
            onMeetingActionClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
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

    @Test
    fun `test that the new chat action is shown on the empty Chats tab when the user has contacts`() {
        setupEmptyScreen(showMeetingTab = false, hasAnyContact = true)

        composeRule.onNodeWithTag(CHAT_LIST_EMPTY_NEW_CHAT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_LIST_EMPTY_INVITE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the invite action is shown on the empty Chats tab when the user has no contacts`() {
        setupEmptyScreen(showMeetingTab = false, hasAnyContact = false)

        composeRule.onNodeWithTag(CHAT_LIST_EMPTY_INVITE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CHAT_LIST_EMPTY_NEW_CHAT_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the empty Chats tab action starts a new chat when the user has contacts`() {
        var newChatClicks = 0
        setupEmptyScreen(
            showMeetingTab = false,
            hasAnyContact = true,
            onNewChatClick = { newChatClicks++ },
        )

        composeRule.onNodeWithTag(CHAT_LIST_EMPTY_NEW_CHAT_TAG).performClick()

        assertThat(newChatClicks).isEqualTo(1)
    }

    @Test
    fun `test that the empty Chats tab action invites a friend when the user has no contacts`() {
        var inviteClicks = 0
        setupEmptyScreen(
            showMeetingTab = false,
            hasAnyContact = false,
            onInviteFriendClick = { inviteClicks++ },
        )

        composeRule.onNodeWithTag(CHAT_LIST_EMPTY_INVITE_TAG).performClick()

        assertThat(inviteClicks).isEqualTo(1)
    }

    @Test
    fun `test that both meeting actions are shown on the empty Meetings tab`() {
        setupEmptyScreen(showMeetingTab = true, hasAnyContact = true)

        composeRule.onNodeWithTag(MEETING_LIST_EMPTY_START_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MEETING_LIST_EMPTY_SCHEDULE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the empty Meetings tab reports the Start action when start meeting is clicked`() {
        val pickedActions = mutableListOf<MeetingAction>()
        setupEmptyScreen(
            showMeetingTab = true,
            hasAnyContact = true,
            onMeetingActionClick = pickedActions::add,
        )

        composeRule.onNodeWithTag(MEETING_LIST_EMPTY_START_TAG).performClick()

        assertThat(pickedActions).containsExactly(MeetingAction.Start)
    }

    @Test
    fun `test that the empty Meetings tab reports the Schedule action when schedule meeting is clicked`() {
        val pickedActions = mutableListOf<MeetingAction>()
        setupEmptyScreen(
            showMeetingTab = true,
            hasAnyContact = true,
            onMeetingActionClick = pickedActions::add,
        )

        composeRule.onNodeWithTag(MEETING_LIST_EMPTY_SCHEDULE_TAG).performClick()

        assertThat(pickedActions).containsExactly(MeetingAction.Schedule)
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
                    onInviteFriendClick = {},
                    onMeetingActionClick = {},
                    onArchivedClick = {},
                    onOpenLinkClick = {},
                    onSearchQueryChange = {},
                    resolveChatRoomActions = { listOf(archiveAction) },
                )
            }
        }
    }

    private fun setupPopulatedScreen(
        showMeetingTab: Boolean,
        onMeetingActionClick: (MeetingAction) -> Unit = {},
        onBackClick: () -> Unit = {},
        hasArchivedChats: Boolean = true,
        isSearchAvailable: Boolean = true,
        onChatStatusSelected: (UserChatStatus) -> Unit = {},
        doNotDisturbOptions: () -> List<ChatPushNotificationMuteOption> = { emptyList() },
        isDoNotDisturbEnabled: Boolean = false,
        onDoNotDisturbEnabledClick: () -> Unit = {},
        onOpenLinkClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides ChatListNavKey::class) {
                AndroidThemeForPreviews {
                    ChatListScreen(
                        uiState = populatedState(
                            hasArchivedChats = hasArchivedChats,
                            isSearchAvailable = isSearchAvailable,
                            isDoNotDisturbEnabled = isDoNotDisturbEnabled,
                        ),
                        showMeetingTab = showMeetingTab,
                        onBackClick = onBackClick,
                        onItemClick = {},
                        onNewChatClick = {},
                        onInviteFriendClick = {},
                        onMeetingActionClick = onMeetingActionClick,
                        onArchivedClick = {},
                        onOpenLinkClick = onOpenLinkClick,
                        onSearchQueryChange = {},
                        onChatStatusSelected = onChatStatusSelected,
                        doNotDisturbOptions = doNotDisturbOptions,
                        onDoNotDisturbEnabledClick = onDoNotDisturbEnabledClick,
                    )
                }
            }
        }
    }

    private fun setupEmptyScreen(
        showMeetingTab: Boolean,
        hasAnyContact: Boolean,
        onNewChatClick: () -> Unit = {},
        onInviteFriendClick: () -> Unit = {},
        onMeetingActionClick: (MeetingAction) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides ChatListNavKey::class) {
                AndroidThemeForPreviews {
                    ChatListScreen(
                        uiState = ChatListUiState.Data(
                            chats = ChatListTabState.Empty,
                            meetings = ChatListTabState.Empty,
                            chatsHaveUnread = false,
                            meetingsHaveUnread = false,
                            hasAnyContact = hasAnyContact,
                            hasArchivedChats = true,
                            isSearchAvailable = true,
                            chatStatus = ChatStatus.Online,
                            isDoNotDisturbEnabled = false,
                            meetingTooltip = MeetingTooltipItem.NONE,
                        ),
                        showMeetingTab = showMeetingTab,
                        onBackClick = {},
                        onItemClick = {},
                        onNewChatClick = onNewChatClick,
                        onInviteFriendClick = onInviteFriendClick,
                        onMeetingActionClick = onMeetingActionClick,
                        onArchivedClick = {},
                        onOpenLinkClick = {},
                        onSearchQueryChange = {},
                    )
                }
            }
        }
    }

    private fun populatedState(
        hasArchivedChats: Boolean = true,
        isSearchAvailable: Boolean = true,
        isDoNotDisturbEnabled: Boolean = false,
    ) = ChatListUiState.Data(
        chats = ChatListTabState.Results(persistentListOf(chatRoomUiItem(1L, "Chat"))),
        meetings = ChatListTabState.Results(persistentListOf(chatRoomUiItem(2L, "Meeting"))),
        chatsHaveUnread = false,
        meetingsHaveUnread = false,
        hasAnyContact = true,
        hasArchivedChats = hasArchivedChats,
        isSearchAvailable = isSearchAvailable,
        chatStatus = ChatStatus.Online,
        isDoNotDisturbEnabled = isDoNotDisturbEnabled,
        meetingTooltip = MeetingTooltipItem.NONE,
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

    @Test
    fun `test that the row actions affordance opens the actions sheet for that chat`() {
        val resolvedFor = mutableListOf<Set<Long>>()
        setupActionableScreen(onActionsResolved = resolvedFor::add)

        composeRule.onAllNodesWithTag(CHAT_ROOM_ITEM_MORE_TAG)[0].performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CHAT_ROOM_ACTIONS_SHEET_TAG).assertIsDisplayed()
        assertThat(resolvedFor).containsExactly(setOf(1L))
    }

    @Test
    fun `test that picking a row action reports it for that chat alone`() {
        val picked = mutableListOf<Pair<Set<Long>, String>>()
        setupActionableScreen(onActionSelected = { ids, action -> picked += ids to action })

        composeRule.onAllNodesWithTag(CHAT_ROOM_ITEM_MORE_TAG)[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_ROOM_ACTIONS_ARCHIVE_TAG).performClick()
        composeRule.waitForIdle()

        assertThat(picked).containsExactly(setOf(1L) to CHAT_ROOM_ACTIONS_ARCHIVE_TAG)
    }

    @Test
    fun `test that the row actions sheet is not restored from saved state`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { RestorableChatListScreen(showMeetingTab = false) }

        composeRule.onAllNodesWithTag(CHAT_ROOM_ITEM_MORE_TAG)[0].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_ROOM_ACTIONS_SHEET_TAG).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(CHAT_ROOM_ACTIONS_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that system back leaves search mode without navigating away`() {
        var backClicks = 0
        setupPopulatedScreen(showMeetingTab = false, onBackClick = { backClicks++ })

        composeRule.onNodeWithTag(CHAT_LIST_SEARCH_ACTION_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_LIST_SEARCH_BAR_TAG).assertIsDisplayed()

        pressBack()

        composeRule.onNodeWithTag(CHAT_LIST_TOP_BAR_TAG).assertIsDisplayed()
        assertThat(backClicks).isEqualTo(0)
    }

    @Test
    fun `test that re-selecting the active tab scrolls its list back to the top`() {
        setupLongMeetingList()

        composeRule.onNodeWithTag(MEETING_LIST_TAG).performScrollToIndex(LONG_LIST_SIZE - 1)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Meeting 1").assertDoesNotExist()

        composeRule.onNodeWithText(meetingsTabLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Meeting 1").assertIsDisplayed()
    }

    @Test
    fun `test that the open link action reports the click`() {
        var openLinkClicks = 0
        setupPopulatedScreen(showMeetingTab = false, onOpenLinkClick = { openLinkClicks++ })

        composeRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_LIST_OPEN_LINK_TAG).performClick()
        composeRule.waitForIdle()

        assertThat(openLinkClicks).isEqualTo(1)
    }

    @Test
    fun `test that the archived action is shown when the user has archived chats`() {
        setupPopulatedScreen(showMeetingTab = false, hasArchivedChats = true)

        composeRule.onNodeWithTag(CHAT_LIST_ARCHIVED_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the archived action is hidden when the user has no archived chats`() {
        setupPopulatedScreen(showMeetingTab = false, hasArchivedChats = false)

        composeRule.onNodeWithTag(CHAT_LIST_ARCHIVED_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the search action is hidden when there is nothing to search`() {
        setupPopulatedScreen(showMeetingTab = false, isSearchAvailable = false)

        composeRule.onNodeWithTag(CHAT_LIST_SEARCH_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the presence is shown as the toolbar subtitle`() {
        setupPopulatedScreen(showMeetingTab = false)

        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.online_status))
            .assertIsDisplayed()
    }

    @Test
    fun `test that tapping the toolbar opens the presence picker`() {
        setupPopulatedScreen(showMeetingTab = false)

        composeRule.onNodeWithTag(CHAT_LIST_TOP_BAR_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(CHAT_STATUS_DIALOG_TAG)[0].assertIsDisplayed()
    }

    @Test
    fun `test that picking a presence reports it`() {
        val picked = mutableListOf<UserChatStatus>()
        setupPopulatedScreen(showMeetingTab = false, onChatStatusSelected = picked::add)

        composeRule.onNodeWithTag(CHAT_LIST_TOP_BAR_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.busy_status))
            .performClick()
        composeRule.waitForIdle()

        assertThat(picked).containsExactly(UserChatStatus.Busy)
    }

    @Test
    fun `test that the do not disturb action opens the mute period picker`() {
        setupPopulatedScreen(
            showMeetingTab = false,
            doNotDisturbOptions = { listOf(ChatPushNotificationMuteOption.Mute1Hour) },
        )

        composeRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_LIST_DO_NOT_DISTURB_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(CHAT_DO_NOT_DISTURB_DIALOG_TAG)[0].assertIsDisplayed()
    }

    @Test
    fun `test that the do not disturb action offers to unmute when every chat is already silenced`() {
        var offered = false
        setupPopulatedScreen(
            showMeetingTab = false,
            doNotDisturbOptions = { listOf(ChatPushNotificationMuteOption.Mute1Hour) },
            isDoNotDisturbEnabled = true,
            onDoNotDisturbEnabledClick = { offered = true },
        )

        composeRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE, useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_LIST_DO_NOT_DISTURB_TAG).performClick()
        composeRule.waitForIdle()

        assertThat(offered).isTrue()
        composeRule.onAllNodesWithTag(CHAT_DO_NOT_DISTURB_DIALOG_TAG).assertCountEquals(0)
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun setupActionableScreen(
        onActionsResolved: (Set<Long>) -> Unit = {},
        onActionSelected: (Set<Long>, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides ChatListNavKey::class) {
                AndroidThemeForPreviews {
                    ChatListScreen(
                        uiState = populatedState(),
                        showMeetingTab = false,
                        onBackClick = {},
                        onItemClick = {},
                        onNewChatClick = {},
                        onInviteFriendClick = {},
                        onMeetingActionClick = {},
                        onArchivedClick = {},
                        onOpenLinkClick = {},
                        onSearchQueryChange = {},
                        resolveChatRoomActions = { chatIds ->
                            onActionsResolved(chatIds)
                            listOf(archiveAction)
                        },
                        onChatRoomActionSelected = onActionSelected,
                    )
                }
            }
        }
    }

    private fun setupLongMeetingList() {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides ChatListNavKey::class) {
                AndroidThemeForPreviews {
                    ChatListScreen(
                        uiState = ChatListUiState.Data(
                            chats = ChatListTabState.Empty,
                            meetings = ChatListTabState.Results(
                                (1..LONG_LIST_SIZE)
                                    .map { chatRoomUiItem(it.toLong(), "Meeting $it") }
                                    .toImmutableList(),
                            ),
                            chatsHaveUnread = false,
                            meetingsHaveUnread = false,
                            hasAnyContact = true,
                            hasArchivedChats = true,
                            isSearchAvailable = true,
                            chatStatus = ChatStatus.Online,
                            isDoNotDisturbEnabled = false,
                            meetingTooltip = MeetingTooltipItem.NONE,
                        ),
                        showMeetingTab = true,
                        onBackClick = {},
                        onItemClick = {},
                        onNewChatClick = {},
                        onInviteFriendClick = {},
                        onMeetingActionClick = {},
                        onArchivedClick = {},
                        onOpenLinkClick = {},
                        onSearchQueryChange = {},
                    )
                }
            }
        }
    }

    private val archiveAction = ChatRoomActionUiItem(
        label = sharedR.string.general_archive,
        icon = IconPack.Medium.Thin.Outline.Archive,
        testTag = CHAT_ROOM_ACTIONS_ARCHIVE_TAG,
        isDestructive = false,
        confirmation = null,
    )

    private companion object {
        const val LONG_LIST_SIZE = 40
    }
}
