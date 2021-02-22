package mega.privacy.android.app.fragments.managerFragments.actionMode

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R

class TransfersActionBarCallBack(private val transfersActionCallback: TransfersActionCallback) :
    ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.transfers_action, menu)
        transfersActionCallback.onCreateActionMode()
        transfersActionCallback.hideTabs(true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val selected = transfersActionCallback.getSelectedTransfers()

        if (selected == 0) {
            menu.findItem(R.id.cab_menu_cancel_transfer).isVisible = false
            menu.findItem(R.id.cab_menu_select_all).isVisible = true
            menu.findItem(R.id.cab_menu_clear_selection).isVisible = false

            return true
        } else if (selected > 0) {
            menu.findItem(R.id.cab_menu_cancel_transfer).isVisible = true
            menu.findItem(R.id.cab_menu_select_all).isVisible =
                !transfersActionCallback.areAllTransfersSelected()
            menu.findItem(R.id.cab_menu_clear_selection).isVisible = true

            return true
        }

        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_menu_cancel_transfer -> {
                transfersActionCallback.cancelTransfers()
                return true
            }
            R.id.cab_menu_select_all -> {
                transfersActionCallback.selectAll()
                return true
            }
            R.id.cab_menu_clear_selection -> {
                transfersActionCallback.clearSelections()
                return true
            }
        }

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        transfersActionCallback.onDestroyActionMode()
        transfersActionCallback.hideTabs(false)
    }

    interface TransfersActionCallback {
        fun onCreateActionMode()
        fun onDestroyActionMode()
        fun cancelTransfers()
        fun selectAll()
        fun clearSelections()
        fun getSelectedTransfers(): Int
        fun areAllTransfersSelected(): Boolean
        fun hideTabs(hide: Boolean)
    }
}