package mega.privacy.android.app.presentation.meeting.chat

import androidx.activity.ComponentActivity
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.CallRecordingViewModel
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_ADD_PARTICIPANTS_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_CLEAR_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_END_CALL_FOR_ALL_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction.Companion.TEST_TAG_VIDEO_CALL_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListUiState
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView
import mega.privacy.android.app.presentation.meeting.chat.view.bottombar.ChatBottomBarViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatGalleryState
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatGalleryViewModel
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.shared.original.core.ui.controls.menus.TAG_MENU_ACTIONS_SHOW_MORE
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.core.test.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class ChatViewTest {
    private val actionPressed = mock<(ChatRoomMenuAction) -> Unit>()

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val chatGalleryViewModel = mock<ChatGalleryViewModel> {
        on { state } doReturn MutableStateFlow(ChatGalleryState())
    }

    private val startTransfersComponentViewModel = mock<StartTransfersComponentViewModel> {
        on { uiState } doReturn MutableStateFlow(StartTransferViewState())
    }

    private val messageListViewModel = mock<MessageListViewModel> {
        on { state } doReturn MutableStateFlow(MessageListUiState())
        on { pagedMessages } doReturn emptyFlow()
    }

    private val chatBottomBarViewModel = mock<ChatBottomBarViewModel>()
    private val callRecordingViewModel = mock<CallRecordingViewModel> {
        on { state } doReturn MutableStateFlow(CallRecordingUIState())
    }
    private val waitingRoomManagementViewModel = mock<WaitingRoomManagementViewModel> {
        on { state } doReturn MutableStateFlow(WaitingRoomManagementState())
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(ChatGalleryViewModel::class.java.canonicalName.orEmpty()) }) } doReturn chatGalleryViewModel
        on { get(argThat<String> { contains(StartTransfersComponentViewModel::class.java.canonicalName.orEmpty()) }) } doReturn startTransfersComponentViewModel
        on { get(argThat<String> { contains(MessageListViewModel::class.java.canonicalName.orEmpty()) }) } doReturn messageListViewModel
        on { get(argThat<String> { contains(ChatBottomBarViewModel::class.java.canonicalName.orEmpty()) }) } doReturn chatBottomBarViewModel
        on { get(argThat<String> { contains(CallRecordingViewModel::class.java.canonicalName.orEmpty()) }) } doReturn callRecordingViewModel
        on { get(argThat<String> { contains(WaitingRoomManagementViewModel::class.java.canonicalName.orEmpty()) }) } doReturn waitingRoomManagementViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }


    private val showNoContactToAddDialog = mock<() -> Unit>()
    private val showEndCallDialog = mock<() -> Unit>()
    private val showClearChatConfirmationDialog = mock<(Boolean) -> Unit>()
    private val showParticipatingInACallDialog = mock<(Long) -> Unit>()
    private val showAllContactsParticipateInChat = mock<() -> Unit>()

    @Test
    fun `test that participating in a call dialog show when click to audio call and user is in another call`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Standard },
                isConnected = true,
                callsInOtherChats = listOf(mock {
                    on { status } doReturn ChatCallStatus.InProgress
                })
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION, true).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(showParticipatingInACallDialog).invoke(any())
    }

    @Test
    fun `test that participating in a call dialog doesn't show when click to audio call and user is in another call`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { ownPrivilege } doReturn ChatRoomPermission.Standard },
                callsInOtherChats = emptyList(),
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TEST_TAG_VIDEO_CALL_ACTION, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.ongoing_call_content))
            .assertDoesNotExist()
    }

    @Test
    fun `test that no contact to add dialog shows when hasAnyContact is false and user clicks add participant menu action`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn true
                },
                hasAnyContact = false,
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_ADD_PARTICIPANTS_ACTION).performClick()
        verify(showNoContactToAddDialog).invoke()
    }

    @Test
    fun `test that all contacts added in a call dialog show when click to add participants to call`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> {
                    on { ownPrivilege } doReturn ChatRoomPermission.Moderator
                    on { isGroup } doReturn true
                },
                allContactsParticipateInChat = true,
                hasAnyContact = true,
                isConnected = true,
            )
        )
        composeTestRule.onNodeWithTag(TAG_MENU_ACTIONS_SHOW_MORE, true).apply {
            assertIsDisplayed()
            performClick()
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ADD_PARTICIPANTS_ACTION).performClick()
        verify(showAllContactsParticipateInChat).invoke()
    }

    @Test
    fun `test that clear chat confirmation dialog show when click clear`() {
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
        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_ACTION).performClick()
        verify(showClearChatConfirmationDialog).invoke(any())
    }

//    }

    @Test
    fun `test that end call for all dialog show when click to end call for all menu option`() {
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
        composeTestRule.onNodeWithTag(TEST_TAG_END_CALL_FOR_ALL_ACTION).performClick()
        verify(showEndCallDialog).invoke()
    }


    @Test
    fun `test that last green label shows if the chat is 1to1 and the contacts last green is not null`() {
        initComposeRuleContent(
            ChatUiState(
                chat = mock<ChatRoom> { on { isGroup } doReturn false },
                isConnected = true,
                userLastGreen = 123456
            )
        )
        composeTestRule.onNodeWithText("Last seen a long time ago").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterialNavigationApi::class)
    private fun initComposeRuleContent(state: ChatUiState) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ChatView(
                    uiState = state,
                    onBackPressed = {},
                    onMenuActionPressed = actionPressed,
                    navHostController = rememberNavController(),
                    bottomSheetNavigator = rememberBottomSheetNavigator(),
                    scaffoldState = rememberScaffoldState(),
                    setSelectedMessages = {},
                    setSelectMode = {},
                    setSelectedReaction = {},
                    setReactionList = {},
                    setPendingAction = {},
                    setAddingReactionTo = {},
                    getApplicableActions = { emptyList() },
                    navigateToFreePlanLimitParticipants = {},
                    showOptionsModal = {},
                    showEmojiModal = {},
                    showNoContactToAddDialog = showNoContactToAddDialog,
                    showParticipatingInACallDialog = showParticipatingInACallDialog,
                    showAllContactsParticipateInChat = showAllContactsParticipateInChat,
                    showGroupOrContactInfoActivity = {},
                    showClearChatConfirmationDialog = showClearChatConfirmationDialog,
                    showMutePushNotificationDialog = {},
                    showEndCallForAllDialog = showEndCallDialog,
                    showToolbarModal = {},
                    showJoinCallDialog = { _, _ -> },
                    showUpgradeToProDialog = {},
                    navigateToChat = {},
                    navigateToContactInfo = {},
                    navigateToMeeting = { _, _, _ -> },
                    navigateToWaitingRoom = {},
                    navigateToReactionInfo = {},
                    navigateToNotSentModal = {},
                    navigateToConversation = {},
                )
            }
        }
    }
}