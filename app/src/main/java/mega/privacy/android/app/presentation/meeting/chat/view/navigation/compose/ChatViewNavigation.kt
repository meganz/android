package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.ChatView

internal const val ConversationRoute = "conversation"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.chatScreen(
    navController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    navigateToFreePlanLimitParticipants: () -> Unit,
    showOptionsModal: (Long) -> Unit,
    showEmojiModal: (Long) -> Unit,
    showNoContactToAddDialog: () -> Unit,
    showParticipatingInACallDialog: (Long) -> Unit,
    showAllContactsParticipateInChat: () -> Unit,
    showGroupOrContactInfoActivity: (ChatUiState) -> Unit,
    showClearChatConfirmationDialog: (Boolean) -> Unit,
    showMutePushNotificationDialog: (Boolean) -> Unit,
    showEndCallForAllDialog: () -> Unit,
    showToolbarModal: () -> Unit,
    showJoinCallDialog: (Boolean, Int) -> Unit,
    showUpgradeToProDialog: () -> Unit,
    navigateToChat: (Long) -> Unit,
    navigateToContactInfo: (String) -> Unit,
    navigateToMeeting: (Long, Boolean, Boolean) -> Unit,
    navigateToWaitingRoom: (Long) -> Unit,
    navigateToReactionInfo: () -> Unit,
    navigateToNotSentModal: () -> Unit,
    navigateToConversation: (Long) -> Unit,
    onBackPress: () -> Unit,
) {
    composable(
        route = ConversationRoute
    ) { backStackEntry ->

        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        ChatView(
            bottomSheetNavigator = bottomSheetNavigator,
            uiState = uiState,
            onBackPressed = onBackPress,
            scaffoldState = scaffoldState,
            onMenuActionPressed = viewModel::handleActionPress,
            enablePasscodeCheck = viewModel::enablePasscodeCheck,
            inviteContactsToChat = viewModel::inviteContactsToChat,
            onInfoToShowConsumed = viewModel::onInfoToShowEventConsumed,
            archiveChat = viewModel::archiveChat,
            unarchiveChat = viewModel::unarchiveChat,
            startCall = viewModel::startCall,
            onCallStarted = viewModel::onCallStarted,
            onWaitingRoomOpened = viewModel::onWaitingRoomOpened,
            onStartOrJoinMeeting = viewModel::onStartOrJoinMeeting,
            onAnswerCall = viewModel::onAnswerCall,
            onSendClick = viewModel::sendTextMessage,
            onJoinChat = viewModel::onJoinChat,
            onSetPendingJoinLink = viewModel::onSetPendingJoinLink,
            onCloseEditing = viewModel::onCloseEditing,
            onAddReaction = viewModel::onAddReaction,
            onDeleteReaction = viewModel::onDeleteReaction,
            onForwardMessages = viewModel::onForwardMessages,
            consumeDownloadEvent = viewModel::consumeDownloadEvent,
            onActionToManageEventConsumed = viewModel::onActionToManageEventConsumed,
            onVoiceClipRecordEvent = viewModel::onVoiceClipRecordEvent,
            onConsumeShouldUpgradeToProPlan = viewModel::onConsumeShouldUpgradeToProPlan,
            setSelectedMessages = viewModel::selectMessages,
            setSelectMode = viewModel::setSelectMode,
            setSelectedReaction = viewModel::setSelectedReaction,
            setReactionList = viewModel::setReactionList,
            setAddingReactionTo = viewModel::setAddingReactionTo,
            setPendingAction = viewModel::setPendingAction,
            getApplicableActions = viewModel::getApplicableActions,
            navigateToFreePlanLimitParticipants = navigateToFreePlanLimitParticipants,
            showOptionsModal = showOptionsModal,
            showEmojiModal = showEmojiModal,
            showNoContactToAddDialog = showNoContactToAddDialog,
            showParticipatingInACallDialog = showParticipatingInACallDialog,
            showAllContactsParticipateInChat = showAllContactsParticipateInChat,
            showGroupOrContactInfoActivity = showGroupOrContactInfoActivity,
            showClearChatConfirmationDialog = showClearChatConfirmationDialog,
            showMutePushNotificationDialog = showMutePushNotificationDialog,
            showEndCallForAllDialog = showEndCallForAllDialog,
            showToolbarModal = showToolbarModal,
            showJoinCallDialog = showJoinCallDialog,
            showUpgradeToProDialog = showUpgradeToProDialog,
            navigateToChat = navigateToChat,
            navigateToContactInfo = navigateToContactInfo,
            navigateToMeeting = navigateToMeeting,
            navigateToWaitingRoom = navigateToWaitingRoom,
            navigateToReactionInfo = navigateToReactionInfo,
            navigateToNotSentModal = navigateToNotSentModal,
            navigateToConversation = navigateToConversation,
            navHostController = navController,
        )
    }
}