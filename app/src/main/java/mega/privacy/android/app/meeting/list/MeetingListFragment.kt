package mega.privacy.android.app.meeting.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.ChatDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentMeetingListBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.chats.ChatTabsFragment
import mega.privacy.android.app.meeting.list.adapter.MeetingItemDetailsLookup
import mega.privacy.android.app.meeting.list.adapter.MeetingItemKeyProvider
import mega.privacy.android.app.meeting.list.adapter.MeetingsAdapter
import mega.privacy.android.app.modalbottomsheet.MeetingBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber

@AndroidEntryPoint
class MeetingListFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): MeetingListFragment =
            MeetingListFragment()
    }

    private lateinit var binding: FragmentMeetingListBinding

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

    override fun onDestroyView() {
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
                            Timber.wtf("Selection size: ${selection.size()}")
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
