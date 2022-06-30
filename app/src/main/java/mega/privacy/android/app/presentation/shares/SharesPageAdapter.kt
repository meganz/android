package mega.privacy.android.app.presentation.shares

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesFragment
import mega.privacy.android.app.presentation.shares.links.LinksFragment
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesFragment
import timber.log.Timber

/**
 * Pager adapter for shares pages
 *
 * @param fa FragmentActivity where the viewPager2 lives
 */
class SharesPageAdapter(private val fa: FragmentActivity, private val context: Context) :
    FragmentStateAdapter(fa) {

    /**
     * Returns the total number of items in the data set held by the adapter.
     * Corresponds to the number of items in SharesTab except the SharesTab.NONE
     *
     * @return the total number of items
     */
    override fun getItemCount(): Int {
        return SharesTab.values().size - 1
    }

    /**
     * Create a new Fragment associated with the specified position.
     *
     * @param position the given position in the adapter
     * @return the instance of the new fragment at the given position
     */
    override fun createFragment(position: Int): Fragment {
        Timber.d("Position: %s", position)
        when (position) {
            SharesTab.INCOMING_TAB.position -> {
                val isF = (context as ManagerActivity).supportFragmentManager.findFragmentByTag(
                    ManagerActivity.FragmentTag.INCOMING_SHARES.tag) as IncomingSharesFragment?
                return isF ?: IncomingSharesFragment()
            }
            SharesTab.OUTGOING_TAB.position -> {
                val osF = (context as ManagerActivity).supportFragmentManager.findFragmentByTag(
                    ManagerActivity.FragmentTag.OUTGOING_SHARES.tag) as OutgoingSharesFragment?
                return osF ?: OutgoingSharesFragment()
            }
            SharesTab.LINKS_TAB.position -> {
                val lF = (context as ManagerActivity).supportFragmentManager.findFragmentByTag(
                    ManagerActivity.FragmentTag.LINKS.tag) as LinksFragment?
                return lF ?: LinksFragment()
            }
            else -> {
                throw Exception("Invalid position")
            }
        }
    }

    /**
     * Return the fragment at the specified position
     *
     * @param position the given position in the adapter
     * @return the fragment at the given position
     */
    fun getPageFragment(position: Int): Fragment? {
        return fa.supportFragmentManager.findFragmentByTag("f${getItemId(position)}")
    }
}