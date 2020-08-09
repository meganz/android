package mega.privacy.android.app.fragments.offline

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.R.id
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.OfflineUtils

class OfflineActionModeCallback(
    private val managerActivity: ManagerActivityLollipop,
    private val fragment: OfflineFragment,
    private val viewMode: OfflineViewModel
) : ActionMode.Callback {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        logDebug("ActionBarCallBack::onActionItemClicked")

        when (item!!.itemId) {
            id.cab_menu_share_out -> {
                OfflineUtils.shareOfflineNodes(managerActivity, viewMode.getSelectedNodes())
                viewMode.clearSelection()
            }
            id.cab_menu_delete -> {
                managerActivity.showConfirmationRemoveSomeFromOffline(viewMode.getSelectedNodes())
                viewMode.clearSelection()
            }
            id.cab_menu_select_all -> {
                viewMode.selectAll()
            }
            id.cab_menu_clear_selection -> {
                viewMode.clearSelection()
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        logDebug("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater
        inflater.inflate(R.menu.offline_browser_action, menu)
        managerActivity.showHideBottomNavigationView(true)
        managerActivity.changeStatusBarColor(Constants.COLOR_STATUS_BAR_ACCENT)
        fragment.checkScroll()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        logDebug("ActionBarCallBack::onPrepareActionMode")

        menu!!.findItem(R.id.cab_menu_select_all).isVisible =
            (viewMode.getSelectedNodesCount()
                    < fragment.getItemCount() - viewMode.placeholderCount)

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        logDebug("ActionBarCallBack::onDestroyActionMode")
        viewMode.clearSelection()
        managerActivity.showHideBottomNavigationView(false)
        managerActivity.changeStatusBarColor(Constants.COLOR_STATUS_BAR_ZERO_DELAY)
        fragment.checkScroll()
    }
}
