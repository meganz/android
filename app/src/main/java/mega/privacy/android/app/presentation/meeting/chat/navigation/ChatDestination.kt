package mega.privacy.android.app.presentation.meeting.chat.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.chatsession.ChatSessionContainer
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.chatViewNavigationGraph
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.navigateToChatViewGraph
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.showGroupOrContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startWaitingRoom
import mega.privacy.android.app.presentation.meeting.chat.view.showPermissionNotAllowedSnackbar
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.resources.R as SharedR
import mega.privacy.mobile.analytics.event.ChatConversationScreenEvent

/**
 * Replaces the former `ChatFragment`. The chat's existing legacy (Navigation 2) graph keeps
 * running inside a nested [NavHost] so only the host is migrated to Navigation 3.
 *
 * @param navigationHandler The scaffold navigation handler. Used to open another chat on top of
 * the current one and to bridge the inner graph to Navigation 3 results.
 * @param megaNavigator The centralized navigator in the :app module.
 */
internal fun EntryProviderScope<NavKey>.chatDestination(
    navigationHandler: NavigationHandler,
    megaNavigator: MegaNavigator,
) {
    entry<ChatLegacyContainerNavKey> { key ->
        // Keep the chat marked as open while this entry is the visible (resumed) destination.
        LifecycleResumeEffect(key.chatId) {
            MegaApplication.openChatId = key.chatId
            Analytics.tracker.trackEvent(ChatConversationScreenEvent)
            onPauseOrDispose { MegaApplication.openChatId = -1L }
        }

        ChatSessionContainer {
            val context = LocalContext.current
            val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navHostController = rememberNavController(bottomSheetNavigator)
            val coroutineScope = rememberCoroutineScope()
            val scaffoldState = rememberScaffoldState()

            // The legacy chat navigation graph still owns the conversation screen and its
            // modals/dialogs; it is hosted by this Navigation3 entry.
            NavHost(
                navController = navHostController,
                startDestination = "start",
                modifier = Modifier.navigationBarsPadding(),
            ) {
                composable("start") {
                    navHostController.navigateToChatViewGraph(
                        chatId = key.chatId,
                        chatLink = key.link,
                        action = key.action,
                        navOptions = navOptions {
                            popUpTo("start") {
                                inclusive = true
                            }
                        },
                    )
                }

                chatViewNavigationGraph(
                    bottomSheetNavigator = bottomSheetNavigator,
                    navController = navHostController,
                    scaffoldState = scaffoldState,
                    onNavigate = navigationHandler::navigate,
                    monitorResult = navigationHandler::monitorResult,
                    clearResult = navigationHandler::clearResult,
                    startMeeting = {
                        startMeetingActivity(context, it)
                    },
                    navigateToInviteContact = {
                        megaNavigator.openInviteContactActivity(context, false)
                    },
                    showGroupOrContactInfoActivity = {
                        showGroupOrContactInfoActivity(context, it)
                    },
                    navigateToChat = { chatId ->
                        navigationHandler.openChatConversation(
                            ChatLegacyContainerNavKey(
                                chatId = chatId,
                                action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                            )
                        )
                    },
                    navigateToContactInfo = {
                        openContactInfoActivity(context, it)
                    },
                    navigateToMeeting = { chatId, enableAudio, enableVideo ->
                        startMeetingActivity(
                            context,
                            chatId,
                            enableAudio,
                            enableVideo,
                        )
                    },
                    navigateToWaitingRoom = {
                        startWaitingRoom(context, it)
                    },
                    onBackPress = {
                        onBackPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                    },
                    onCameraPermissionDenied = {
                        showPermissionNotAllowedSnackbar(
                            context,
                            coroutineScope,
                            scaffoldState.snackbarHostState,
                            SharedR.string.camera_denied_info_message,
                        )
                    },
                    enablePasscodeCheck = {},
                    navigateToWebSite = { context.launchUrl(it) },
                )
            }
        }
    }
}

/**
 * Opens a chat conversation, replacing any chat already on top of the back stack so conversations
 * never stack on each other (matching the legacy `ChatFragment` `replace()` behaviour). When the
 * top is the chat tabs (or the stack has no chat), there is nothing to pop, so it simply pushes —
 * back then returns to the tabs.
 */
internal fun NavigationHandler.openChatConversation(key: ChatLegacyContainerNavKey) {
    navigate(
        key,
        mega.privacy.android.navigation.contract.navOptions {
            popUpTo(ChatLegacyContainerNavKey::class) { inclusive = true }
        },
    )
}
