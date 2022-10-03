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
class FileExplorerPagerAdapter(fm: FragmentManager, lifeCycle: Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {

    var tabRemoved = false

    private var mChatFragment: Fragment? = null
    private var mCloudFragment: Fragment? = null
    private var mIncomingFragment: Fragment? = null

    override fun createFragment(position: Int): Fragment = when (position) {
        1 -> incomingFragment
        2 -> chatFragment
        0 -> cloudFragment
        else -> cloudFragment
    }

    override fun getItemCount(): Int = if (!tabRemoved) PAGE_COUNT else PAGE_COUNT - 1

    private val chatFragment: Fragment
        get() = mChatFragment ?: ChatExplorerFragment.newInstance()
            .also { mChatFragment = it }

    private val incomingFragment: Fragment
        get() = mIncomingFragment ?: IncomingSharesExplorerFragment.newInstance()
            .also { mIncomingFragment = it }

    private val cloudFragment: Fragment
        get() = mCloudFragment ?: CloudDriveExplorerFragment.newInstance()
            .also { mCloudFragment = it }

    companion object {
        private const val PAGE_COUNT = 3
    }
}