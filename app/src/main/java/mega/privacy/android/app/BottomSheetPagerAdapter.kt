package mega.privacy.android.app

import android.util.SparseArray
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment
import java.lang.ref.WeakReference

class BottomSheetPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments: SparseArray<WeakReference<Fragment>> = SparseArray()

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                val fragment = RecentsFragment.newInstance()
                fragments.put(0, WeakReference(fragment))
                return fragment
            }
            1,2 -> {
                val fragment = OfflineFragmentLollipop()
                fragments.put(position, WeakReference(fragment))
                return fragment
            }
            else -> OfflineFragmentLollipop()
        }
    }

    public fun getViewAt(pos: Int): View? {
        val weakReference = fragments.get(pos)
        return weakReference?.get()?.view
    }
}