package mega.privacy.android.app.utils.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter

abstract class DiffFragmentStateAdapter<T> : FragmentStateAdapter {

    private val differ: AsyncListDiffer<T>

    protected constructor(
        fragmentActivity: FragmentActivity,
        diffCallback: DiffUtil.ItemCallback<T>
    ) : super(fragmentActivity) {
        differ = AsyncListDiffer(this, diffCallback)
    }

    protected constructor(
        fragment: Fragment,
        diffCallback: DiffUtil.ItemCallback<T>
    ) : super(fragment) {
        differ = AsyncListDiffer(this, diffCallback)
    }

    protected constructor(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        diffCallback: DiffUtil.ItemCallback<T>
    ) : super(fragmentManager, lifecycle) {
        differ = AsyncListDiffer(this, diffCallback)
    }

    @JvmOverloads
    fun submitList(list: List<T>?, commitCallback: Runnable? = null) {
        differ.submitList(list, commitCallback)
    }

    fun getCurrentList(): List<T> =
        differ.currentList

    protected fun getItem(position: Int): T =
        differ.currentList[position]

    override fun getItemCount(): Int =
        differ.currentList.size
}
