package mega.privacy.android.app

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment

class BottomSheetPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RecentsFragment.newInstance()
            1,2 -> OfflineFragmentLollipop()
            else -> HomepageFragment2.newInstance("", "")
        }
    }
}