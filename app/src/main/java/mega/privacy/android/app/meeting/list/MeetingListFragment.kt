package mega.privacy.android.app.meeting.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMeetingListBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.chats.ChatTabsFragment
import mega.privacy.android.app.meeting.list.adapter.MeetingsAdapter
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util

@AndroidEntryPoint
class MeetingListFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): MeetingListFragment =
            MeetingListFragment()
    }

    private lateinit var binding: FragmentMeetingListBinding

    private val viewModel by viewModels<MeetingListViewModel>()
    private val adapter by lazy { MeetingsAdapter(::onItemClick, ::onItemMoreClick) }

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

    override fun onDestroyView() {
        binding.list.clearOnScrollListeners()
        super.onDestroyView()
    }

    private fun setupView() {
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.canScrollVertically(RecyclerView.NO_POSITION)) {
                    (activity as? ManagerActivity?)?.changeAppBarElevation(Util.isDarkMode(context))
                    (parentFragment as? ChatTabsFragment?)?.showElevation(Util.isDarkMode(context))
                } else {
                    (activity as? ManagerActivity?)?.changeAppBarElevation(false)
                    (parentFragment as? ChatTabsFragment?)?.showElevation(false)
                }
            }
        })
        binding.listScroller.setRecyclerView(binding.list)
    }

    private fun setupObservers() {
        viewModel.getMeetings().observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.viewEmpty.isVisible = items.isNullOrEmpty()
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

    private fun onItemClick(chatId: Long) {
        viewModel.signalChatPresence()
        val intent = Intent(context, ChatActivity::class.java).apply {
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
            putExtra(Constants.CHAT_ID, chatId)
        }
        startActivity(intent)
    }

    private fun onItemMoreClick(chatId: Long) {
        MeetingListBottomSheetDialogFragment.newInstance(chatId)
            .show(childFragmentManager)
    }
}
