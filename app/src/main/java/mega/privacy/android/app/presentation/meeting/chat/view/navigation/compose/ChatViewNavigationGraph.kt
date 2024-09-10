package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.utils.Constants
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal const val chatIdArg = "chatId"
internal const val chatActionArg = "chatAction"
internal const val chatLinkArg = "link"
internal const val ChatNavigationRoutePattern =
    "chat/{$chatIdArg}/{$chatActionArg}?link={$chatLinkArg}"

internal fun chatNavigationRoutePattern(chatId: Long, action: String, link: String?) =
    "chat/$chatId/$action?link=$link"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.chatViewNavigationGraph(
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    startMeeting: (Long) -> Unit,
    navigateToInviteContact: () -> Unit,
    showGroupOrContactInfoActivity: (ChatUiState) -> Unit,
    navigateToChat: (Long) -> Unit,
    navigateToContactInfo: (String) -> Unit,
    navigateToMeeting: (Long, Boolean, Boolean) -> Unit,
    navigateToWaitingRoom: (Long) -> Unit,
    onBackPress: () -> Unit,
    onCameraPermissionDenied : () -> Unit,
) {
    navigation(
        startDestination = ConversationRoute,
        route = ChatNavigationRoutePattern,
        arguments = listOf(
            navArgument(chatIdArg) { NavType.LongType },
            navArgument(chatActionArg) { NavType.StringType },
            navArgument(chatLinkArg) {
                NavType.StringType
                nullable = true
            },
        ),
    ) {

        chatScreen(
            navController = navController,
            bottomSheetNavigator = bottomSheetNavigator,
            scaffoldState = scaffoldState,
            navigateToFreePlanLimitParticipants = navController::navigateToFreePlanLimitsParticipantsDialog,
            showOptionsModal = navController::navigateToMessageOptionsModal,
            showEmojiModal = navController::navigateToEmojiPickerModal,
            showNoContactToAddDialog = navController::navigateToNoContactToAddDialog,
            showParticipatingInACallDialog = navController::navigateToInCallDialog,
            showAllContactsParticipateInChat = navController::navigateToAllParticipantsDialog,
            showGroupOrContactInfoActivity = showGroupOrContactInfoActivity,
            showClearChatConfirmationDialog = navController::navigateToClearChatConfirmationDialog,
            showMutePushNotificationDialog = navController::navigateToMutePushNotificationDialog,
            showEndCallForAllDialog = navController::navigateToEndCallForAllDialog,
            showToolbarModal = navController::navigateToChatToolbarModal,
            showJoinCallDialog = { isGroup, otherCalls ->
                navController.navigateToJoinCallDialog(
                    isGroup,
                    otherCalls
                )
            },
            showUpgradeToProDialog = navController::navigateToUpgradeToProPlanModal,
            navigateToChat = navigateToChat,
            navigateToContactInfo = navigateToContactInfo,
            navigateToMeeting = navigateToMeeting,
            navigateToWaitingRoom = navigateToWaitingRoom,
            navigateToReactionInfo = navController::navigateToReactionInfoModal,
            navigateToNotSentModal = navController::navigateToMessageNotSentModal,
            navigateToConversation = navController::navigateToChatViewGraph,
            onBackPress = onBackPress,
        )

        chatFileModal(navController = navController) {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        messageOptionsModal(
            navController = navController,
            navigateToEmojiPicker = navController::navigateToEmojiPickerModal,
        ) {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        emojiPickerModal(navController) {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        messageNotSentModal(navController) {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        reactionInfoModal(
            navController = navController,
            scaffoldState = scaffoldState,
        ) {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        chatToolbarModal(
            navController = navController,
            scaffoldState = scaffoldState,
            onCameraPermissionDenied = onCameraPermissionDenied
        ) {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        upgradeToProPlanModal {
            navController.popBackStack(
                ConversationRoute,
                false
            )
        }

        inCallDialog(
            navController = navController,
            startMeeting = startMeeting,
        )

        mutePushNotificationDialog(navController)

        noContactToAddDialog(
            navController = navController,
            navigateToInviteContact = navigateToInviteContact
        )

        allParticipantsDialog(
            navController = navController,
            onNavigateToInviteContact = navigateToInviteContact
        )

        clearChatConfirmationDialog(navController)

        endCallForAllDialog(navController)

        chatLocationDialog(navController)

        joinCallDialog(navController)

        freePlanLimitsParticipantsDialog(navController)
    }
}

internal fun NavHostController.navigateToChatViewGraph(
    chatId: Long,
    action: String = Constants.ACTION_CHAT_SHOW_MESSAGES,
    chatLink: String? = null,
    navOptions: NavOptions? = null,
) {
    val encodedUrl = chatLink?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }
    navigate(
        chatNavigationRoutePattern(
            chatId = chatId,
            action = action,
            link = encodedUrl
        ), navOptions
    )
}

internal class ChatArgs(val chatId: Long, val action: String, val link: String?) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(
                chatId = checkNotNull(savedStateHandle.get<String>(chatIdArg)?.toLongOrNull()),
                action = checkNotNull(savedStateHandle.get<String>(chatActionArg)),
                link = savedStateHandle.get(chatLinkArg)
            )
}

@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(navController: NavHostController): T {
    val navGraphRoute = destination.parent?.route ?: return hiltViewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}