package mega.privacy.android.app.presentation.meeting.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_IN
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.list.view.MeetingListView
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

@AndroidEntryPoint
class MeetingListFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): MeetingListFragment =
            MeetingListFragment()
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private var actionMode: ActionMode? = null

    private val viewModel by viewModels<MeetingListViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.getState().collectAsStateWithLifecycle()
                AndroidTheme(isDark = mode.isDarkMode()) {
                    MeetingListView(
                        state = uiState,
                        onItemClick = ::onItemClick,
                        onItemMoreClick = ::onItemMoreClick,
                        onItemSelected = ::onItemSelected,
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.collectFlow(viewModel.getState(), Lifecycle.State.RESUMED) { state ->
            if (state.selectedMeetings.isNotEmpty()) {
                if (actionMode == null) {
                    actionMode = (activity as? AppCompatActivity?)
                        ?.startSupportActionMode(buildActionMode())
                } else if (state.selectedMeetings.size.toString() != actionMode?.title) {
                    actionMode?.invalidate()
                }
                actionMode?.title = state.selectedMeetings.size.toString()
            } else {
                actionMode?.finish()
            }

            state.currentCallChatId?.let {
                viewModel.removeCurrentCall()
                launchChatCallScreen(state.currentCallChatId)
            }

            state.snackBar?.let { stringResId ->
                (requireActivity() as ManagerActivity).showSnackbar(
                    Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE,
                    view,
                    getString(stringResId),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
                viewModel.updateSnackBar(null)
            }
        }
    }

    private fun buildActionMode(): ActionMode.Callback =
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.recent_chat_action, menu)
                menu.findItem(R.id.cab_menu_delete)?.isVisible = false // Not implemented
                menu.findItem(R.id.cab_menu_unarchive)?.isVisible = false // Not implemented
                menu.findItem(R.id.chat_list_leave_chat_layout)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val currentState = viewModel.getState().value
                val selectedItems = currentState.selectedMeetings.mapNotNull { chatId ->
                    currentState.meetings.firstOrNull { it.chatId == chatId }
                }

                menu.apply {
                    findItem(R.id.cab_menu_unmute)?.isVisible = selectedItems.all { it.isMuted }
                    findItem(R.id.cab_menu_mute)?.isVisible = selectedItems.all { !it.isMuted }
                    findItem(R.id.cab_menu_select_all)?.isVisible =
                        selectedItems.size != currentState.meetings.size
                    findItem(R.id.chat_list_leave_chat_layout)?.isVisible =
                        selectedItems.all { it.isActive }
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
                when (item.itemId) {
                    R.id.cab_menu_select_all -> {
                        val allItems = viewModel.getState().value.meetings.map { it.chatId }
                        viewModel.onItemsSelected(allItems)
                        true
                    }
                    R.id.cab_menu_unselect_all -> {
                        clearSelections()
                        true
                    }
                    R.id.cab_menu_mute -> {
                        ChatUtil.createMuteNotificationsAlertDialogOfChats(
                            requireActivity(),
                            viewModel.getState().value.selectedMeetings
                        )
                        clearSelections()
                        true
                    }
                    R.id.cab_menu_unmute -> {
                        viewModel.getState().value.selectedMeetings.forEach { id ->
                            MegaApplication.getPushNotificationSettingManagement()
                                .controlMuteNotificationsOfAChat(
                                    requireContext(),
                                    Constants.NOTIFICATIONS_ENABLED,
                                    id
                                )
                        }
                        clearSelections()
                        true
                    }
                    R.id.cab_menu_archive -> {
                        val selectedMeetings = viewModel.getState().value.selectedMeetings
                        viewModel.archiveChats(selectedMeetings)
                        clearSelections()
                        true
                    }
                    R.id.chat_list_leave_chat_layout -> {
                        val selectedMeetings = viewModel.getState().value.selectedMeetings
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
                        clearSelections()
                        true
                    }
                    else -> false
                }

            override fun onDestroyActionMode(mode: ActionMode) {
                clearSelections()
                actionMode = null
            }
        }

    /**
     * Launch chat call screen
     *
     * @param chatId    Chat id to be shown
     */
    private fun launchChatCallScreen(chatId: Long) {
        activity?.startActivity(
            Intent(context, MeetingActivity::class.java).apply {
                action = MEETING_ACTION_IN
                putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
                putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, true)
                putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    /**
     * Set search query
     *
     * @param query Search query string
     */
    fun setSearchQuery(query: String?) {
        viewModel.setSearchQuery(query)
    }

    private fun onItemClick(chatId: Long) {
        viewModel.signalChatPresence()

        val intent = Intent(context, ChatActivity::class.java).apply {
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
            putExtra(Constants.CHAT_ID, chatId)
        }
        startActivity(intent)
    }

    private fun onItemMoreClick(chatId: Long) {
        MeetingListBottomSheetDialogFragment.newInstance(chatId).show(childFragmentManager)
    }

    private fun onItemSelected(chatId: Long) {
        viewModel.onItemSelected(chatId)
    }

    /**
     * Clear item selections
     */
    fun clearSelections() {
        viewModel.clearSelection()
    }

    /**
     * Scroll to the top of the list
     */
    fun scrollToTop() {
        viewModel.scrollToTop()
    }
}
