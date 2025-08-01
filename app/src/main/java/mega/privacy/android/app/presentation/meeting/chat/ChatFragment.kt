package mega.privacy.android.app.presentation.meeting.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.chatsession.ChatSessionContainer
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.chatViewNavigationGraph
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose.navigateToChatViewGraph
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatFragment
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.showGroupOrContactInfoActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startMeetingActivity
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.startWaitingRoom
import mega.privacy.android.app.presentation.meeting.chat.view.showPermissionNotAllowedSnackbar
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.ChatConversationScreenEvent
import javax.inject.Inject

@AndroidEntryPoint
internal class ChatFragment : Fragment() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        MegaApplication.openChatId = requireActivity().intent.getLongExtra(Constants.CHAT_ID, -1L)

        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }

            SessionContainer {
                ChatSessionContainer {
                    OriginalTheme(isDark = mode.isDarkMode()) {
                        PasscodeContainer(
                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                            canLock = { passcodeEnabled },
                            content = {
                                PsaContainer {
                                    val bottomSheetNavigator = rememberBottomSheetNavigator()
                                    val navHostController =
                                        rememberNavController(bottomSheetNavigator)
                                    val chatId =
                                        requireActivity().intent.getLongExtra(Constants.CHAT_ID, -1)
                                    val action =
                                        requireActivity().intent.getStringExtra(EXTRA_ACTION)
                                            ?: Constants.ACTION_CHAT_SHOW_MESSAGES
                                    val chatLink =
                                        requireActivity().intent.getStringExtra(EXTRA_LINK)
                                    val coroutineScope = rememberCoroutineScope()

                                    val scaffoldState = rememberScaffoldState()

                                    //Real chat navigation graph implementation should include the chat list screen and use that as the default route,
                                    NavHost(
                                        navController = navHostController,
                                        startDestination = "start",
                                        modifier = Modifier.navigationBarsPadding()
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
                                                }
                                            )
                                        }

                                        chatViewNavigationGraph(
                                            bottomSheetNavigator = bottomSheetNavigator,
                                            navController = navHostController,
                                            scaffoldState = scaffoldState,
                                            startMeeting = {
                                                startMeetingActivity(
                                                    requireContext(),
                                                    it
                                                )
                                            },
                                            navigateToInviteContact = {
                                                megaNavigator.openInviteContactActivity(
                                                    requireContext(),
                                                    false
                                                )
                                            },
                                            showGroupOrContactInfoActivity = {
                                                showGroupOrContactInfoActivity(
                                                    context,
                                                    it
                                                )
                                            },
                                            navigateToChat = { openChatFragment(context, it) },
                                            navigateToContactInfo = {
                                                openContactInfoActivity(
                                                    context,
                                                    it
                                                )
                                            },
                                            navigateToMeeting = { chatId, enableAudio, enableVideo ->
                                                startMeetingActivity(
                                                    context,
                                                    chatId,
                                                    enableAudio,
                                                    enableVideo
                                                )
                                            },
                                            navigateToWaitingRoom = {
                                                startWaitingRoom(
                                                    context,
                                                    it
                                                )
                                            },
                                            onBackPress = { requireActivity().supportFinishAfterTransition() },
                                            onCameraPermissionDenied = {
                                                showPermissionNotAllowedSnackbar(
                                                    context,
                                                    coroutineScope,
                                                    scaffoldState.snackbarHostState,
                                                    R.string.chat_attach_pick_from_camera_deny_permission
                                                )
                                            },
                                            navigateToStorageSettings = {
                                                megaNavigator.openSettings(
                                                    requireActivity(),
                                                    storageTargetPreference
                                                )
                                            },
                                            enablePasscodeCheck = { passcodeEnabled = true }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MegaApplication.openChatId = requireActivity().intent.getLongExtra(Constants.CHAT_ID, -1L)
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
