package mega.privacy.android.app.presentation.chat.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.chat.dialog.AskForDisplayOverActivity
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.view.ChatTabsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.mobile.analytics.event.ChatScreenEvent
import mega.privacy.mobile.analytics.event.ChatsTabEvent
import mega.privacy.mobile.analytics.event.MeetingsTabEvent
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * Chat tabs fragment containing Chat and Meeting fragment
 */
@AndroidEntryPoint
class ChatTabsFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): ChatTabsFragment =
            ChatTabsFragment()
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private var actionMode: ActionMode? = null
    private var currentTab: ChatTab = ChatTab.CHATS

    private val viewModel by viewModels<ChatTabsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val chatsTabState by viewModel.getState().collectAsStateWithLifecycle()
                AndroidTheme(isDark = mode.isDarkMode()) {
                    ChatTabsView(
                        state = chatsTabState,
                        onTabSelected = ::onTabSelected,
                        onItemClick = ::onItemClick,
                        onItemMoreClick = ::onItemMoreClick,
                        onItemSelected = ::onItemSelected,
                        onScrollInProgress = ::onScrollInProgress,
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        startActivity(Intent(requireContext(), AskForDisplayOverActivity::class.java))

        viewLifecycleOwner.collectFlow(viewModel.getState(), Lifecycle.State.RESUMED) { state ->
            if (state.selectedIds.isNotEmpty()) {
                if (actionMode == null) {
                    actionMode = (activity as? AppCompatActivity?)
                        ?.startSupportActionMode(buildActionMode())
                } else if (state.selectedIds.size.toString() != actionMode?.title) {
                    actionMode?.invalidate()
                }
                actionMode?.title = state.selectedIds.size.toString()
            } else {
                actionMode?.finish()
            }

            when (state.currentChatStatus) {
                ChatStatus.Online -> R.string.online_status
                ChatStatus.Offline -> R.string.offline_status
                ChatStatus.Away -> R.string.away_status
                ChatStatus.Busy -> R.string.busy_status
                ChatStatus.NoNetworkConnection -> R.string.error_server_connection_problem
                ChatStatus.Reconnecting -> R.string.invalid_connection_state
                ChatStatus.Connecting -> R.string.chat_connecting
                else -> null
            }?.let { subtitle ->
                (activity as? AppCompatActivity?)?.supportActionBar?.setSubtitle(subtitle)
            }

            state.currentCallChatId?.let {
                viewModel.removeCurrentCall()
                launchChatCallScreen(state.currentCallChatId)
            }

            state.snackBar?.let { snackBar ->
                (activity as? BaseActivity?)?.showSnackbar(
                    snackBar.type,
                    view,
                    snackBar.getMessage(resources),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                viewModel.updateSnackBar(null)
            }
        }

        view.post {
            (activity as? ManagerActivity?)?.showHideBottomNavigationView(false)
            (activity as? ManagerActivity?)?.showFabButton()
            (activity as? ManagerActivity?)?.invalidateOptionsMenu()
            (activity as? ManagerActivity?)?.findViewById<View>(R.id.toolbar)?.setOnClickListener {
                (activity as? ManagerActivity?)?.showPresenceStatusDialog()
            }
            setupMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        Analytics.tracker.trackEvent(ChatScreenEvent)
        Firebase.crashlytics.log("Screen: ${ChatScreenEvent.eventName}")
    }

    override fun onStop() {
        viewModel.clearSearchQuery()
        super.onStop()
    }

    override fun onDestroyView() {
        (activity as? ManagerActivity?)?.findViewById<View>(R.id.toolbar)?.setOnClickListener(null)
        super.onDestroyView()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_chat_tabs, menu)
                menu.findItem(R.id.menu_search)?.setupSearchView(viewModel::setSearchQuery)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = true
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onItemClick(chatId: Long) {
        viewModel.signalChatPresence()

        val intent = Intent(context, ChatActivity::class.java).apply {
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
            putExtra(Constants.CHAT_ID, chatId)
        }
        startActivity(intent)
    }

    private fun onItemMoreClick(chatRoomItem: ChatRoomItem) {
        ChatListBottomSheetDialogFragment.newInstance(chatRoomItem.chatId).show(childFragmentManager)
    }

    private fun onItemSelected(chatId: Long) {
        viewModel.onItemSelected(chatId)
    }

    private fun onTabSelected(selectedTab: ChatTab) {
        currentTab = selectedTab

        viewModel.clearSelection()
        viewModel.clearSearchQuery()
        activity?.invalidateMenu()
        view?.hideKeyboard()

        if (currentTab == ChatTab.CHATS) {
            Analytics.tracker.trackEvent(ChatsTabEvent)
        } else {
            viewModel.requestMeetings()
            Analytics.tracker.trackEvent(MeetingsTabEvent)
        }
    }

    /**
     * Check if meeting tab is shown
     */
    fun isMeetingTabShown(): Boolean =
        currentTab == ChatTab.MEETINGS

    /**
     * Launch chat call screen
     *
     * @param chatId    Chat id to be shown
     */
    private fun launchChatCallScreen(chatId: Long) {
        activity?.startActivity(
            Intent(context, MeetingActivity::class.java).apply {
                action = MeetingActivity.MEETING_ACTION_IN
                putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
                putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, true)
                putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun onScrollInProgress(scrolling: Boolean) {
        LiveEventBus.get<Boolean>(Constants.EVENT_FAB_CHANGE).post(!scrolling)
    }

    private fun buildActionMode(): ActionMode.Callback =
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.fragment_chat_tabs_selection, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val currentState = viewModel.getState().value
                val currentItems = if (!isMeetingTabShown()) {
                    currentState.chats
                } else {
                    currentState.meetings
                }
                val selectedItems = currentState.selectedIds.mapNotNull { chatId ->
                    currentItems.firstOrNull { it.chatId == chatId }
                }

                menu.apply {
                    findItem(R.id.menu_chat_unmute)?.isVisible =
                        selectedItems.all { it.isMuted }
                    findItem(R.id.menu_chat_mute)?.isVisible =
                        selectedItems.all { !it.isMuted }
                    findItem(R.id.menu_chat_select_all)?.isVisible =
                        selectedItems.size != currentItems.size
                    findItem(R.id.menu_chat_leave)?.isVisible =
                        selectedItems.all {
                            it is ChatRoomItem.GroupChatRoomItem
                                    || it is ChatRoomItem.MeetingChatRoomItem
                        }
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
                when (item.itemId) {
                    R.id.menu_chat_select_all -> {
                        val allItems = if (!isMeetingTabShown()) {
                            viewModel.getState().value.chats.map(ChatRoomItem::chatId)
                        } else {
                            viewModel.getState().value.meetings.map(ChatRoomItem::chatId)
                        }
                        viewModel.onItemsSelected(allItems)
                        true
                    }

                    R.id.menu_chat_unselect_all -> {
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_mute -> {
                        ChatUtil.createMuteNotificationsAlertDialogOfChats(
                            requireActivity(),
                            viewModel.getState().value.selectedIds
                        )
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_unmute -> {
                        viewModel.getState().value.selectedIds.forEach { id ->
                            MegaApplication.getPushNotificationSettingManagement()
                                .controlMuteNotificationsOfAChat(
                                    requireContext(),
                                    Constants.NOTIFICATIONS_ENABLED,
                                    id
                                )
                        }
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_archive -> {
                        val selectedMeetings = viewModel.getState().value.selectedIds.toLongArray()
                        viewModel.archiveChats(*selectedMeetings)
                        viewModel.clearSelection()
                        true
                    }

                    R.id.menu_chat_leave -> {
                        val selectedMeetings = viewModel.getState().value.selectedIds
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.ThemeOverlay_Mega_MaterialAlertDialog
                        )
                            .setTitle(getString(R.string.title_confirmation_leave_group_chat))
                            .setMessage(getString(R.string.confirmation_leave_group_chat))
                            .setPositiveButton(getString(R.string.general_leave)) { _, _ ->
                                viewModel.leaveChats(selectedMeetings)
                            }
                            .setNegativeButton(
                                getString(R.string.general_cancel),
                                null
                            )
                            .show()
                        viewModel.clearSelection()
                        true
                    }

                    else -> false
                }

            override fun onDestroyActionMode(mode: ActionMode) {
                viewModel.clearSelection()
                actionMode = null
            }
        }
}
