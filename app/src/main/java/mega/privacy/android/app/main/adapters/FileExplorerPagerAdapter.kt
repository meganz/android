package mega.privacy.android.app.main.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.main.CloudDriveExplorerFragment
import mega.privacy.android.app.main.IncomingSharesExplorerFragment
import mega.privacy.android.app.main.megachat.ChatExplorerFragment

/**
 * Adapter for FileExplorerActivity.
 *
 * @property tabRemoved True if should remove a tab, false otherwise.
 */
class FileExplorerPagerAdapter(private val fm: FragmentManager, lifeCycle: Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {

    var tabRemoved = false

    private val fragments = mapOf(
        0 to CloudDriveExplorerFragment(),
        1 to IncomingSharesExplorerFragment(),
        2 to ChatExplorerFragment()
    )

    override fun createFragment(position: Int): Fragment {
        return fragments[position] as Fragment
    }

    override fun getItemId(position: Int): Long = fragments[position].hashCode().toLong()

    override fun containsItem(itemId: Long): Boolean {
        return fragments.filterValues { it.hashCode().toLong() == itemId }.isNotEmpty()
    }

    override fun getItemCount(): Int = if (!tabRemoved) PAGE_COUNT else PAGE_COUNT - 1

    fun getFragment(position: Int): Fragment? = fm.findFragmentByTag("f${getItemId(position)}")

    companion object {
        private const val PAGE_COUNT = 3
    }
}