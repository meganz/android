package mega.privacy.android.app.fragments.homepage.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeFragment
import mega.privacy.android.app.presentation.recentactions.RecentActionsComposeFragment

class BottomSheetPagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {

    private val tabFragmentMap = hashMapOf(
        RECENT_INDEX to RecentActionsComposeFragment::class.java,
        OFFLINE_INDEX to OfflineComposeFragment::class.java
    )

    override fun getItemCount(): Int {
        return tabFragmentMap.size
    }

    override fun createFragment(position: Int): Fragment {
        var fragment: Fragment? = null

        try {
            fragment = tabFragmentMap[position]?.newInstance() as Fragment
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }

        return fragment!!
    }

    companion object {
        const val RECENT_INDEX = 0
        const val OFFLINE_INDEX = 1
    }
}