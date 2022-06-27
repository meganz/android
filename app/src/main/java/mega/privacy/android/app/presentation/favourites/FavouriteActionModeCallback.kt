package mega.privacy.android.app.presentation.favourites

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import timber.log.Timber

/**
 * Action mode callback is use for feature regarding to favourite
 * @param mainActivity ManagerActivity
 * @param viewModel FavouritesViewModel
 */
class FavouriteActionModeCallback(
    private val mainActivity: ManagerActivity,
    private val viewModel: FavouritesViewModel,
) :
    ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.cloud_storage_action, menu)
        menu?.toggleAllMenuItemsVisibility(false)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        item?.run {
            val nodeHandles = arrayListOf<Long>().apply {
                addAll(
                    viewModel.getItemsSelected().values.map {
                        it.handle
                    })
            }
            val selectedNodes = viewModel.getItemsSelected().values.map { it.node }

            if (itemId != R.id.cab_menu_select_all) {
                viewModel.clearSelections()
            }

            when (itemId) {
                R.id.cab_menu_download -> {
                    mainActivity.saveNodesToDevice(selectedNodes, false, false, false, false)
                }
                R.id.cab_menu_copy -> {
                    NodeController(mainActivity).chooseLocationToCopyNodes(nodeHandles)
                }
                R.id.cab_menu_move -> {
                    NodeController(mainActivity).chooseLocationToMoveNodes(nodeHandles)
                }
                R.id.cab_menu_share_out -> {
                    MegaNodeUtil.shareNodes(mainActivity, selectedNodes)
                }
                R.id.cab_menu_share_link -> {
                    Timber.d("Public link option")
                    LinksUtil.showGetLinkActivity(mainActivity, nodeHandles.toLongArray())
                }
                R.id.cab_menu_send_to_chat -> {
                    Timber.d("Send files to chat")
                    mainActivity.attachNodesToChats(selectedNodes)
                }
                R.id.cab_menu_trash -> {
                    mainActivity.askConfirmationMoveToRubbish(
                        nodeHandles
                    )
                }
                R.id.cab_menu_select_all -> viewModel.selectAll()
                R.id.cab_menu_remove_favourites -> {
                    viewModel.favouritesRemoved(nodeHandles)
                }
                R.id.cab_menu_rename -> {
                    mainActivity.showRenameDialog(selectedNodes.first())
                }
                R.id.cab_menu_share_folder -> {
                    NodeController(mainActivity).selectContactToShareFolders(nodeHandles)
                }
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // When the action mode is finished, clear the selections
        viewModel.clearSelections()
    }
}