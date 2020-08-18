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
    private val viewModel: OfflineViewModel
) : ActionMode.Callback {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        logDebug("ActionBarCallBack::onActionItemClicked")

        when (item!!.itemId) {
            id.cab_menu_share_out -> {
                OfflineUtils.shareOfflineNodes(managerActivity, viewModel.getSelectedNodes())
                viewModel.clearSelection()
            }
            id.cab_menu_delete -> {
                managerActivity.showConfirmationRemoveSomeFromOffline(viewModel.getSelectedNodes())
                viewModel.clearSelection()
            }
            id.cab_menu_select_all -> {
                viewModel.selectAll()
            }
            id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
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
            (viewModel.getSelectedNodesCount()
                    < fragment.getItemCount() - viewModel.placeholderCount)

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        logDebug("ActionBarCallBack::onDestroyActionMode")
        viewModel.clearSelection()
        managerActivity.showHideBottomNavigationView(false)
        managerActivity.changeStatusBarColor(Constants.COLOR_STATUS_BAR_ZERO_DELAY)
        fragment.checkScroll()
    }
}
