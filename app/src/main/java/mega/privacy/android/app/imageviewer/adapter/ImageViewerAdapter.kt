package mega.privacy.android.app.imageviewer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import mega.privacy.android.app.imageviewer.ImageViewerPageFragment
import mega.privacy.android.app.imageviewer.data.ImageAdapterItem
import mega.privacy.android.app.utils.view.DiffFragmentStateAdapter

/**
 * Image Viewer adapter based on a list of ImageAdapterItems that contains ImageViewerPageFragment.
 */
class ImageViewerAdapter(
    val enableZoom: Boolean,
    fragmentManager: FragmentManager,
    viewLifecycle: Lifecycle,
) : DiffFragmentStateAdapter<ImageAdapterItem>
    (fragmentManager, viewLifecycle, ImageAdapterItem.DiffCallback()) {

    override fun createFragment(position: Int): Fragment =
        ImageViewerPageFragment.newInstance(getItem(position).id, enableZoom)

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun containsItem(itemId: Long): Boolean =
        getCurrentList().any { itemId == it.id }
}
