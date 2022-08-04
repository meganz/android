package mega.privacy.android.app.imageviewer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import mega.privacy.android.app.imageviewer.ImageViewerPageFragment
import mega.privacy.android.app.utils.LongDiffCallback
import mega.privacy.android.app.utils.view.DiffFragmentStateAdapter

/**
 * Image Viewer adapter based on a list of longs that creates a ImageViewerPageFragment.
 */
class ImageViewerAdapter(fragmentManager: FragmentManager, viewLifecycle: Lifecycle) :
    DiffFragmentStateAdapter<Long>(fragmentManager, viewLifecycle, LongDiffCallback()) {

    override fun createFragment(position: Int): Fragment =
        ImageViewerPageFragment.newInstance(getItem(position))

    override fun getItemId(position: Int): Long =
        getItem(position)

    override fun containsItem(itemId: Long): Boolean =
        getCurrentList().contains(itemId)
}
