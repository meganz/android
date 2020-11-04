package mega.privacy.android.app.fragments.managerFragments.actionMode

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R

class TransfersActionBarCallBack(private val transfersActionInterface: TransfersActionInterface) :
    ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.transfers_action, menu)
        transfersActionInterface.onCreateActionMode()
        return true;
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val selected = transfersActionInterface.getSelectedTransfers()

        if (selected == 0) {
            menu.findItem(R.id.cab_menu_cancel_transfer).isVisible = false
            menu.findItem(R.id.cab_menu_select_all).isVisible = true
            menu.findItem(R.id.cab_menu_clear_selection).isVisible = false

            return true
        } else if (selected > 0) {
            menu.findItem(R.id.cab_menu_cancel_transfer).isVisible = true
            menu.findItem(R.id.cab_menu_select_all).isVisible =
                !transfersActionInterface.areAllTransfersSelected()
            menu.findItem(R.id.cab_menu_clear_selection).isVisible = true

            return true
        }

        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_menu_cancel_transfer -> {
                transfersActionInterface.cancelTransfers()
                return true
            }
            R.id.cab_menu_select_all -> {
                transfersActionInterface.selectAll()
                return true
            }
            R.id.cab_menu_clear_selection -> {
                transfersActionInterface.clearSelections()
                return true
            }
        }

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        transfersActionInterface.onDestroyActionMode()
    }

    interface TransfersActionInterface {
        fun onCreateActionMode()
        fun onDestroyActionMode()
        fun cancelTransfers()
        fun selectAll()
        fun clearSelections()
        fun getSelectedTransfers() : Int
        fun areAllTransfersSelected() : Boolean
    }
}