package mega.privacy.android.app.imageviewer.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import mega.privacy.android.app.imageviewer.ImageViewerPageFragment
import mega.privacy.android.app.utils.HandleDiffCallback
import mega.privacy.android.app.utils.view.DiffFragmentStateAdapter

class ImageViewerAdapter(activity: FragmentActivity) :
    DiffFragmentStateAdapter<Long>(activity, HandleDiffCallback()) {

    override fun createFragment(position: Int): Fragment =
        ImageViewerPageFragment.newInstance(getItem(position))

    override fun getItemId(position: Int): Long =
        getItem(position)
}
