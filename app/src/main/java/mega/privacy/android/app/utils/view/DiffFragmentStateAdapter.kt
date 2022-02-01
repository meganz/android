package mega.privacy.android.app.utils.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * FragmentStateAdapter with DiffUtil implementation for ViewPager2.
 * This class is a convenience wrapper around AsyncListDiffer that implements
 * Adapter common default behavior for item access and counting.
 *
 * Took from: https://gist.github.com/Gnzlt/7e8a23ba0c3b046ed33c824b284d7270
 *
 * @param T     Type of the Lists this Adapter will receive.
 */
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

    /**
     * Set the new list to be displayed.
     * If a List is already being displayed, a diff will be computed on a background thread, which
     * will dispatch Adapter.notifyItem events on the main thread.
     * The commit callback can be used to know when the List is committed, but note that it
     * may not be executed. If List B is submitted immediately after List A, and is
     * committed directly, the callback associated with List A will not be run.
     *
     * @param list The new list to be displayed.
     * @param commitCallback Optional runnable that is executed when the List is committed, if
     *                       it is committed.
     */
    @JvmOverloads
    fun submitList(list: List<T>?, commitCallback: Runnable? = null) {
        differ.submitList(list, commitCallback)
    }

    /**
     * Get the current List.
     * If a <code>null</code> List, or no List has been submitted, an empty list will be returned.
     * The returned list may not be mutated - mutations to content must be done through
     * {@link #submitList(List)}.
     *
     * @return The list currently being displayed.
     */
    fun getCurrentList(): List<T> =
        differ.currentList

    /**
     * Get the item at a specific position.
     *
     * @param position
     * @return  The item.
     */
    protected fun getItem(position: Int): T =
        differ.currentList[position]

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int =
        differ.currentList.size
}
