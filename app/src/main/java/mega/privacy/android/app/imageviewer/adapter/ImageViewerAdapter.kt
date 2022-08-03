package mega.privacy.android.app.imageviewer.adapter

import androidx.fragment.app.Fragment
import mega.privacy.android.app.imageviewer.ImageViewerPageFragment
import mega.privacy.android.app.utils.LongDiffCallback
import mega.privacy.android.app.utils.view.DiffFragmentStateAdapter

/**
 * Image Viewer adapter based on a list of longs that creates a ImageViewerPageFragment.
 */
class ImageViewerAdapter(fragment: Fragment) :
    DiffFragmentStateAdapter<Long>(fragment, LongDiffCallback()) {

    override fun createFragment(position: Int): Fragment =
        ImageViewerPageFragment.newInstance(getItem(position))

    override fun getItemId(position: Int): Long =
        getItem(position)
}
