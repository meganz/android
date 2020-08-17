package mega.privacy.android.app.components

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.lollipop.managerSections.FavouritesFragment
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment

class BottomSheetPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val tabFragmentMap = object : HashMap<Int, Class<*>>() {
        init {
            put(RECENT_INDEX, RecentsFragment::class.java)
            put(FAVOURITES_INDEX, FavouritesFragment::class.java)
            put(OFFLINE_INDEX, OfflineFragmentLollipop::class.java)
        }
    }

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
        const val FAVOURITES_INDEX = 1
        const val OFFLINE_INDEX = 2
    }
}