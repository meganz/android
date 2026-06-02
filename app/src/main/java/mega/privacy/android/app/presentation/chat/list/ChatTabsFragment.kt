package mega.privacy.android.app.presentation.chat.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Chat tabs fragment containing Chat and Meeting tabs.
 *
 * Thin shell that delegates all chat logic to [ChatTabsScreen].
 */
@AndroidEntryPoint
class ChatTabsFragment : Fragment() {

    @Inject
    lateinit var navigator: MegaNavigator

    companion object {
        private const val EXTRA_SHOW_MEETING_TAB = "EXTRA_SHOW_MEETING_TAB"

        @JvmStatic
        fun newInstance(showMeetingTab: Boolean): ChatTabsFragment =
            ChatTabsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(EXTRA_SHOW_MEETING_TAB, showMeetingTab)
                }
            }
    }

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val showMeetingTab by lazy {
        arguments?.getBoolean(EXTRA_SHOW_MEETING_TAB, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val mode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                MegaAppContainer(themeMode = mode, finishOnSessionRefresh = false) {
                    ChatTabsScreen(
                        showMeetingTab = showMeetingTab,
                        createNewChat = arguments?.getBoolean(
                            ChatActivity.CREATE_NEW_CHAT,
                            false
                        ) ?: false,
                        onNavigateToChat = { chatId ->
                            navigator.openChat(
                                context = requireActivity(),
                                chatId = chatId,
                                action = Constants.ACTION_CHAT_SHOW_MESSAGES
                            )
                        },
                        onFinish = { activity?.finish() },
                    )
                }
            }
        }
}
