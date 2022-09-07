package mega.privacy.android.app.meeting.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentChatTabsBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.RecentChatsFragment
import mega.privacy.android.app.meeting.chats.adapter.ChatTabsPageAdapter
import mega.privacy.android.app.meeting.chats.adapter.ChatTabsPageAdapter.Tabs.CHAT
import mega.privacy.android.app.meeting.list.MeetingListFragment
import mega.privacy.android.app.utils.AlertsAndWarnings.showAskForDisplayOverDialog
import mega.privacy.android.app.utils.ColorUtils.setElevationWithColor
import mega.privacy.android.app.utils.StringResourcesUtils

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

    private lateinit var binding: FragmentChatTabsBinding

    private val viewModel by activityViewModels<ChatTabsViewModel>()
    private val toolbarElevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }
    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                (activity as? ManagerActivity?)?.closeSearchView()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentChatTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        view.post {
            showAskForDisplayOverDialog(requireActivity())
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val position = viewModel.getCurrentPosition()
        binding.pager.setCurrentItem(position, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.setCurrentPosition(binding.pager.currentItem)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.pager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    private fun setupView() {
        binding.pager.apply {
            adapter = ChatTabsPageAdapter(this@ChatTabsFragment)

            TabLayoutMediator(binding.tabs, this) { tab, position ->
                tab.text = if (position == CHAT.ordinal) {
                    StringResourcesUtils.getString(R.string.section_chat)
                } else {
                    StringResourcesUtils.getString(R.string.context_meeting)
                }
            }.attach()

            registerOnPageChangeCallback(pageChangeCallback)
        }

        binding.root.post {
            (activity as? ManagerActivity?)?.showHideBottomNavigationView(false)
            (activity as? ManagerActivity?)?.showFabButton()
            (activity as? ManagerActivity?)?.invalidateOptionsMenu()
        }
    }

    /**
     * Set search query
     *
     * @param query Search query string
     */
    fun setSearchQuery(query: String) {
        childFragmentManager.fragments.firstOrNull { it.isResumed }?.let { fragment ->
            when (fragment) {
                is RecentChatsFragment -> fragment.filterChats(query, false)
                is MeetingListFragment -> fragment.setSearchQuery(query)
            }
        }
    }

    /**
     * Show toolbar elevation
     *
     * @param show  Flag to either show or hide toolbar elevation
     */
    fun showElevation(show: Boolean) {
        binding.tabs.setElevationWithColor(if (show) toolbarElevation else 0F)
    }

    /**
     * Get existing RecentChatsFragment
     *
     * @return  RecentChatsFragment
     */
    fun getRecentChatsFragment(): RecentChatsFragment? =
        childFragmentManager.fragments.find { it is RecentChatsFragment } as? RecentChatsFragment?
}
