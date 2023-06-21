package mega.privacy.android.app.fragments.homepage

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaShare
import timber.log.Timber

class ActionModeCallback constructor(
    private val mainActivity: ManagerActivity,
    private val viewModel: ActionModeViewModel,
    private val megaApi: MegaApiAndroid,
) : ActionMode.Callback {

    var nodeCount = 0

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val selectedNodeItems = viewModel.selectedNodes.value ?: return false
        val selectedNodes = selectedNodeItems.mapNotNull { it.node }
        val nodesHandles = ArrayList(selectedNodes.map { it.handle })

        if (item!!.itemId != R.id.cab_menu_select_all) {
            viewModel.clearSelection()   // include cab_menu_clear_selection
        }

        when (item.itemId) {
            R.id.cab_menu_download -> {
                viewModel.executeTransfer {
                    mainActivity.saveNodesToDevice(
                        selectedNodes,
                        false,
                        false,
                        false,
                        false
                    )
                }
            }
            R.id.cab_menu_copy -> {
                NodeController(mainActivity).chooseLocationToCopyNodes(nodesHandles)
            }
            R.id.cab_menu_move -> {
                NodeController(mainActivity).chooseLocationToMoveNodes(nodesHandles)
            }
            R.id.cab_menu_share_out -> {
                MegaNodeUtil.shareNodes(mainActivity, selectedNodes)
            }
            R.id.cab_menu_share_link, R.id.cab_menu_edit_link -> {
                Timber.d("Public link option")
                if (nodesHandles.isNotEmpty()) {
                    if (nodesHandles.size > 1) {
                        LinksUtil.showGetLinkActivity(mainActivity, nodesHandles.toLongArray())
                    } else {
                        LinksUtil.showGetLinkActivity(mainActivity, nodesHandles[0])
                    }
                }
            }
            R.id.cab_menu_remove_link -> {
                Timber.d("Remove public link option")
                if (selectedNodes.size == 1) {
                    mainActivity.showConfirmationRemovePublicLink(selectedNodes[0])
                }
            }
            R.id.cab_menu_send_to_chat -> {
                Timber.d("Send files to chat")
                mainActivity.attachNodesToChats(selectedNodes)
            }
            R.id.cab_menu_trash -> {
                mainActivity.askConfirmationMoveToRubbish(
                    nodesHandles
                )
            }
            R.id.cab_menu_select_all -> viewModel.selectAll()
            R.id.cab_menu_remove_favourites -> {
                viewModel.removeFavourites(megaApi, selectedNodes)
            }
        }

        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.let {
            val inflater = it.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
        }
        mainActivity.changeAppBarElevation(true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val selectedNodes = viewModel.selectedNodes.value!!.map { it.node }
        val control = CloudStorageOptionControlUtil.Control()
        val areAllNotTakenDown = selectedNodes.areAllNotTakenDown()

        menu?.findItem(R.id.cab_menu_share_link)?.title =
            mainActivity.resources.getQuantityString(R.plurals.get_links, selectedNodes.size)

        if (areAllNotTakenDown) {
            if (selectedNodes.size == 1
                && megaApi.checkAccessErrorExtended(
                    selectedNodes[0],
                    MegaShare.ACCESS_OWNER
                ).errorCode
                == MegaError.API_OK
            ) {
                selectedNodes[0]?.let {
                    if (it.isExported) {
                        control.manageLink().setVisible(true).showAsAction =
                            MenuItem.SHOW_AS_ACTION_ALWAYS
                        control.removeLink().isVisible = true
                    } else {
                        control.link.setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                }
            }
        }

        viewModel.selectedNodes.value?.run {
            val selectedMegaNodes = map { it.node }
            control.trash().isVisible = MegaNodeUtil.canMoveToRubbish(selectedMegaNodes)
            control.selectAll().isVisible = selectedMegaNodes.size < nodeCount
        }

        if (selectedNodes.size > 1) {
            control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        }

        if (areAllNotTakenDown) {
            control.sendToChat().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            control.copy().isVisible = true
        } else {
            control.saveToDevice().isVisible = false
            control.trash().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        }

        control.move().isVisible = true

        if (mainActivity.isInAlbumContentPage) {
            manageVisibilityForFavouriteAlbum(control)
        }

        CloudStorageOptionControlUtil.applyControl(menu, control)

        return true
    }

    /**
     * Manage Visibility when in Favourite Album page
     */
    private fun manageVisibilityForFavouriteAlbum(control: CloudStorageOptionControlUtil.Control) {
        control.link.isVisible = false
        control.move().isVisible = false
        control.copy().isVisible = false
        control.trash().isVisible = false
        control.removeLink().isVisible = false
        control.manageLink().isVisible = false
        control.removeFavourites().isVisible = true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        viewModel.clearSelection()
        viewModel.actionModeDestroy()
        mainActivity.changeAppBarElevation(false)
    }
}