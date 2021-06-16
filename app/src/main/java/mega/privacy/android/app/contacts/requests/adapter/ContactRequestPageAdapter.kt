package mega.privacy.android.app.contacts.requests.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.contacts.requests.ContactRequestsPageFragment

class ContactRequestPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { INCOMING, OUTGOING }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        if (position == Tabs.INCOMING.ordinal) {
            ContactRequestsPageFragment.newInstance(false)
        } else {
            ContactRequestsPageFragment.newInstance(true)
        }
}
