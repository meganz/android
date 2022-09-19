package mega.privacy.android.app.meeting.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentMeetingListBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.chats.ChatTabsFragment
import mega.privacy.android.app.meeting.list.adapter.MeetingItemDetailsLookup
import mega.privacy.android.app.meeting.list.adapter.MeetingItemKeyProvider
import mega.privacy.android.app.meeting.list.adapter.MeetingsAdapter
import mega.privacy.android.app.modalbottomsheet.MeetingBottomSheetDialogFragment
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util

@AndroidEntryPoint
class MeetingListFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): MeetingListFragment =
            MeetingListFragment()
    }

    private lateinit var binding: FragmentMeetingListBinding
    private var actionMode: ActionMode? = null

    private val viewModel by viewModels<MeetingListViewModel>()
    private val meetingsAdapter by lazy { MeetingsAdapter(::onItemClick, ::onItemMoreClick) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMeetingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        checkElevation()
    }

    override fun onPause() {
        actionMode?.finish()
        super.onPause()
    }

    override fun onDestroyView() {
        meetingsAdapter.tracker?.clearSelection()
        binding.list.clearOnScrollListeners()
        super.onDestroyView()
    }

    private fun setupView() {
        binding.list.apply {
            adapter = meetingsAdapter
            setHasFixedSize(true)
            addItemDecoration(ChatDividerItemDecoration(context))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkElevation()
                }
            })

            meetingsAdapter.tracker = SelectionTracker.Builder(
                MeetingListFragment::class.java.simpleName,
                this,
                MeetingItemKeyProvider(meetingsAdapter),
                MeetingItemDetailsLookup(this),
                StorageStrategy.createLongStorage()
            ).withSelectionPredicate(SelectionPredicates.createSelectAnything()).build()
                .apply {
                    addObserver(object : SelectionTracker.SelectionObserver<Long>() {
                        override fun onSelectionChanged() {
                            super.onSelectionChanged()
                            if (selection.size() > 0) {
                                if (actionMode == null) {
                                    actionMode = (activity as? AppCompatActivity?)
                                        ?.startSupportActionMode(buildActionMode())
                                } else {
                                    actionMode?.invalidate()
                                }
                                actionMode?.title = selection.size().toString()
                            } else {
                                actionMode?.finish()
                            }
                        }

                        override fun onSelectionRefresh() {
                            super.onSelectionRefresh()
                            actionMode?.invalidate()
                        }
                    })
                }
        }

        binding.listScroller.setRecyclerView(binding.list)
        binding.btnNewMeeting.setOnClickListener {
            MeetingBottomSheetDialogFragment.newInstance(true)
                .show(childFragmentManager, MeetingBottomSheetDialogFragment.TAG)
        }
    }

    private fun setupObservers() {
        viewModel.getMeetings().observe(viewLifecycleOwner) { items ->
            val currentFirstChat = meetingsAdapter.currentList.firstOrNull()?.chatId
            meetingsAdapter.submitList(items) {
                if (currentFirstChat != items.firstOrNull()?.chatId) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
            if (items.isNullOrEmpty()) {
                val searchQueryEmpty = viewModel.isSearchQueryEmpty()
                binding.viewEmpty.isVisible = searchQueryEmpty
                binding.viewEmptySearch.root.isVisible = !searchQueryEmpty
            } else {
                binding.viewEmpty.isVisible = false
                binding.viewEmptySearch.root.isVisible = false
            }
        }
    }

    /**
     * Set search query
     *
     * @param query Search query string
     */
    fun setSearchQuery(query: String?) {
        viewModel.setSearchQuery(query)
        viewModel.signalChatPresence()
    }

    /**
     * Check tabs proper elevation given the current RecyclerView's position
     */
    private fun checkElevation() {
        if (binding.list.canScrollVertically(RecyclerView.NO_POSITION)) {
            (activity as? ManagerActivity?)?.changeAppBarElevation(Util.isDarkMode(context))
            (parentFragment as? ChatTabsFragment?)?.showElevation(true)
        } else {
            (activity as? ManagerActivity?)?.changeAppBarElevation(false)
            (parentFragment as? ChatTabsFragment?)?.showElevation(false)
        }
    }

    private fun buildActionMode(): ActionMode.Callback =
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.menuInflater.inflate(R.menu.recent_chat_action, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.findItem(R.id.cab_menu_delete).isVisible = false // Not implemented
                menu.findItem(R.id.cab_menu_unarchive).isVisible = false // Not implemented

                val selectedItems = meetingsAdapter.tracker?.selection
                    ?.map { id -> meetingsAdapter.currentList.first { it.chatId == id } }
                    ?: return true

                if (selectedItems.size == meetingsAdapter.currentList.size) {
                    menu.findItem(R.id.cab_menu_select_all).isVisible = false
                }
                if (selectedItems.all { it.hasPermissions }) {
                    menu.findItem(R.id.chat_list_leave_chat_layout).isVisible = true
                }
                if (selectedItems.all { it.isMuted }) {
                    menu.findItem(R.id.cab_menu_unmute).isVisible = true
                    menu.findItem(R.id.cab_menu_mute).isVisible = false
                } else if (selectedItems.all { !it.isMuted }) {
                    menu.findItem(R.id.cab_menu_mute).isVisible = true
                    menu.findItem(R.id.cab_menu_unmute).isVisible = false
                } else {
                    menu.findItem(R.id.cab_menu_mute).isVisible = false
                    menu.findItem(R.id.cab_menu_unmute).isVisible = false
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.cab_menu_select_all -> {
                        val allItems = meetingsAdapter.currentList.map { it.chatId }
                        meetingsAdapter.tracker?.setItemsSelected(allItems, true)
                        true
                    }
                    R.id.cab_menu_unselect_all -> {
                        meetingsAdapter.tracker?.clearSelection()
                        true
                    }
                    R.id.cab_menu_mute -> {
                        val chats = meetingsAdapter.tracker?.selection?.toList() ?: return true
                        ChatUtil.createMuteNotificationsAlertDialogOfChats(requireActivity(), chats)
                        meetingsAdapter.tracker?.clearSelection()
                        true
                    }
                    R.id.cab_menu_unmute -> {
                        meetingsAdapter.tracker?.selection?.forEach { chatId ->
                            MegaApplication.getPushNotificationSettingManagement()
                                .controlMuteNotificationsOfAChat(
                                    requireContext(),
                                    Constants.NOTIFICATIONS_ENABLED,
                                    chatId)
                        }
                        meetingsAdapter.tracker?.clearSelection()
                        true
                    }
                    R.id.cab_menu_archive -> {
                        val chatsToArchive = meetingsAdapter.tracker?.selection?.toList() ?: return true
                        viewModel.archiveChats(chatsToArchive)
                        meetingsAdapter.tracker?.clearSelection()
                        true
                    }
                    R.id.chat_list_leave_chat_layout -> {
                        val chatsToLeave = meetingsAdapter.tracker?.selection?.toList() ?: return true
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                            .setTitle(StringResourcesUtils.getString(R.string.title_confirmation_leave_group_chat))
                            .setMessage(StringResourcesUtils.getString(R.string.confirmation_leave_group_chat))
                            .setPositiveButton(StringResourcesUtils.getString(R.string.general_leave)) { _, _ ->
                                viewModel.leaveChats(chatsToLeave)
                            }
                            .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                            .show()
                        meetingsAdapter.tracker?.clearSelection()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                meetingsAdapter.tracker?.clearSelection()
                actionMode = null
            }
        }

    private fun onItemClick(chatId: Long) {
        viewModel.signalChatPresence()

        val intent = Intent(context, ChatActivity::class.java).apply {
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
            putExtra(Constants.CHAT_ID, chatId)
        }
        startActivity(intent)

        (activity as? ManagerActivity?)?.closeSearchView()
    }

    private fun onItemMoreClick(chatId: Long) {
        MeetingListBottomSheetDialogFragment.newInstance(chatId).show(childFragmentManager)
    }
}
