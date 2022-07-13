package mega.privacy.android.app.meeting.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.ContactRequestsFragment
import mega.privacy.android.app.databinding.FragmentChatTabsBinding
import mega.privacy.android.app.main.megachat.RecentChatsFragment
import mega.privacy.android.app.meeting.chats.adapter.ChatTabsPageAdapter
import mega.privacy.android.app.meeting.chats.adapter.ChatTabsPageAdapter.Tabs.CHAT
import mega.privacy.android.app.utils.StringResourcesUtils

@AndroidEntryPoint
class ChatTabsFragment : Fragment() {

    companion object {
        private const val STATE_PAGER_POSITION = "STATE_PAGER_POSITION"

        @JvmStatic
        fun newInstance(): ChatTabsFragment =
            ChatTabsFragment()
    }

    private lateinit var binding: FragmentChatTabsBinding

    private val toolbarElevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }

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

        if (savedInstanceState?.containsKey(ContactRequestsFragment.STATE_PAGER_POSITION) == true) {
            setViewPagerPosition(savedInstanceState.getInt(ContactRequestsFragment.STATE_PAGER_POSITION))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PAGER_POSITION, binding.pager.currentItem)
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
        }
    }

    /**
     * Show toolbar elevation
     *
     * @param show  Flag to either show or hide toolbar elevation
     */
    fun showElevation(show: Boolean) {
        binding.tabs.elevation = if (show) toolbarElevation else 0F
    }

    fun getRecentChatsFragment(): RecentChatsFragment? =
        childFragmentManager.fragments.find { it is RecentChatsFragment } as? RecentChatsFragment

    /**
     * Update ViewPager current position
     *
     * @param pagerPosition     Position to be shown
     */
    private fun setViewPagerPosition(pagerPosition: Int) {
        binding.pager.post { binding.pager.currentItem = pagerPosition }
    }
}
