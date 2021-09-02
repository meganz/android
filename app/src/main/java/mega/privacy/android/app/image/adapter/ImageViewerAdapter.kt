package mega.privacy.android.app.image.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import mega.privacy.android.app.image.ImageViewerPageFragment
import mega.privacy.android.app.image.data.ImageItem

class ImageViewerAdapter(activity: FragmentActivity) :
    DiffFragmentStateAdapter<ImageItem>(activity, ImageItem.DiffCallback()) {

    override fun createFragment(position: Int): Fragment =
        ImageViewerPageFragment.newInstance(getItem(position).handle)
}
