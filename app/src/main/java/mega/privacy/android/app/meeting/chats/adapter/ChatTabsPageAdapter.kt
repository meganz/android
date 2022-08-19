package mega.privacy.android.app.meeting.chats.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.main.megachat.RecentChatsFragment
import mega.privacy.android.app.meeting.list.MeetingListFragment

/**
 * ViewPager's Adapter to handle chat child fragments.
 *
 * @param fragment  Fragment needed to instantiate FragmentStateAdapter
 */
class ChatTabsPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { CHAT, MEETING }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        if (position == Tabs.CHAT.ordinal) {
            RecentChatsFragment()
        } else {
            MeetingListFragment.newInstance()
        }
}
