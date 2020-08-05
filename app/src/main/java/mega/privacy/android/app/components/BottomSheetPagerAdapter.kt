package mega.privacy.android.app.components

import android.util.SparseArray
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.fragments.offline.OfflineFragment
import mega.privacy.android.app.lollipop.managerSections.FavouritesFragment
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment
import java.lang.ref.WeakReference

class BottomSheetPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragmentRefs: SparseArray<WeakReference<Fragment>> = SparseArray()

    private val tabFragmentMap = object : HashMap<Int, Class<*>>() {
        init {
            put(RECENT_INDEX, RecentsFragment::class.java)
            put(FAVOURITES_INDEX, FavouritesFragment::class.java)
            put(OFFLINE_INDEX, OfflineFragment::class.java)
        }
    }

    override fun getItemCount(): Int {
        return tabFragmentMap.size
    }

    override fun createFragment(position: Int): Fragment {
        var fragment: Fragment? = null

        try {
            fragment = tabFragmentMap[position]?.newInstance() as Fragment
            if (position == OFFLINE_INDEX) {
                OfflineFragment.setArgs(fragment as OfflineFragment, true)
            }
            fragmentRefs.put(position, WeakReference(fragment))
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }

        return fragment!!
    }

    fun getViewAt(pos: Int): View? {
        val weakReference = fragmentRefs.get(pos)
        return weakReference?.get()?.view
    }

    companion object {
        const val RECENT_INDEX = 0
        const val FAVOURITES_INDEX = 1
        const val OFFLINE_INDEX = 2
    }
}