package mega.privacy.android.app.presentation.meeting.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.components.chatsession.ChatSessionContainer
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.navigation.ChatLegacyContainerNavKey
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.chatViewNavigationGraph
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.navigateToChatViewGraph
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.showGroupOrContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startWaitingRoom
import mega.privacy.android.app.presentation.meeting.chat.view.showPermissionNotAllowedSnackbar
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.resources.R as SharedR
import mega.privacy.mobile.analytics.event.ChatConversationScreenEvent
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatFragment : Fragment() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        MegaApplication.openChatId =
            requireActivity().intent.getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1L)

        setContent {
            val mode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val action = requireActivity().intent.getStringExtra(EXTRA_ACTION)
                ?: Constants.ACTION_CHAT_SHOW_MESSAGES

            // Fragment-hosted ComposeViews don't pick up the activity's NavigationEventDispatcher
            // via the View tree during initial composition, so NavDisplay would crash. Provide the
            // activity (which implements NavigationEventDispatcherOwner) so back press is properly
            // bridged to the system: NavDisplay's handler disables itself when its stack has one
            // entry, letting back fall through to the inner legacy NavHost / activity.
            val navigationEventDispatcherOwner =
                LocalActivity.current as NavigationEventDispatcherOwner
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
            ) {
                LegacyActivityScaffold(
                    container = { content ->
                        MegaAppContainer(themeMode = mode, content = content)
                    },
                    initialKey = ChatLegacyContainerNavKey,
                    navigationResultManager = navigationResultManager,
                    featureDestinations = featureDestinations,
                    appDialogDestinations = appDialogDestinations,
                    onEmptyBackStack = {
                        if (isAdded) requireActivity().onBackPressedDispatcher.onBackPressed()
                    },
                ) { navigationHandler, _ ->
                    entry<ChatLegacyContainerNavKey> {
                        ChatSessionContainer {
                            val bottomSheetNavigator = rememberBottomSheetNavigator()
                            val navHostController = rememberNavController(bottomSheetNavigator)
                            val chatId = requireActivity().intent
                                .getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1)
                            val chatLink = requireActivity().intent.getStringExtra(EXTRA_LINK)
                            val coroutineScope = rememberCoroutineScope()
                            val scaffoldState = rememberScaffoldState()

                            //Real chat navigation graph implementation should include the chat list screen and use that as the default route,
                            NavHost(
                                navController = navHostController,
                                startDestination = "start",
                                modifier = Modifier.navigationBarsPadding(),
                            ) {
                                composable("start") {
                                    navHostController.navigateToChatViewGraph(
                                        chatId = chatId,
                                        chatLink = chatLink,
                                        action = action,
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
                                        startMeetingActivity(requireContext(), it)
                                    },
                                    navigateToInviteContact = {
                                        megaNavigator.openInviteContactActivity(
                                            requireContext(),
                                            false,
                                        )
                                    },
                                    showGroupOrContactInfoActivity = {
                                        showGroupOrContactInfoActivity(context, it)
                                    },
                                    navigateToChat = { openChatFragment(context, it) },
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
                                        requireActivity().onBackPressedDispatcher.onBackPressed()
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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MegaApplication.openChatId =
            requireActivity().intent.getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1L)
        Analytics.tracker.trackEvent(ChatConversationScreenEvent)
    }

    override fun onPause() {
        super.onPause()
        MegaApplication.openChatId = -1L
    }

    override fun onDestroy() {
        super.onDestroy()
        MegaApplication.openChatId = -1L
    }
}
