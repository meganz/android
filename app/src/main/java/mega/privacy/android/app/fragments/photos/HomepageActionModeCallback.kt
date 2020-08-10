package mega.privacy.android.app.fragments.photos

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.*
import java.util.*
import javax.inject.Inject

// ActionMode callback belongs to UI layer, same to the Fragment
class HomepageActionModeCallback @Inject constructor(
    @ActivityContext private val context: Context,
    private val viewModel: ActionModeViewModel,
    private val photosViewModel: PhotosViewModel,
    private val megaApi: MegaApiAndroid
) : ActionMode.Callback {

    private var mainActivity = context as ManagerActivityLollipop

    private var configItemsFun = fun(menu: Menu) {
        Log.i("Alex", "defaultConfigActionItems")
        val selectedNodes = viewModel.selectedNodes.value!!.map { it.node }
        val control = CloudStorageOptionControlUtil.Control()

        if (selectedNodes.size == 1
            && megaApi.checkAccess(selectedNodes[0], MegaShare.ACCESS_OWNER).errorCode
            == MegaError.API_OK
        ) {
            if (selectedNodes[0]!!.isExported) {
                control.manageLink().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                control.removeLink().isVisible = true
                Log.i("Alex", "removeLink")
            } else {
                control.link.setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
        }

        viewModel.selectedNodes.value?.run {
            val selectedMegaNodes = map { it.node }
            control.trash().isVisible = MegaNodeUtil.canMoveToRubbish(selectedMegaNodes)

            photosViewModel.items.value?.let {
                control.selectAll().isVisible = selectedMegaNodes.size < it[it.size - 1].index + 1
            }
        }

        if (selectedNodes.size > 1) {
            control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        }

        control.sendToChat().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        control.move().isVisible = true
        control.copy().isVisible = true

        menu.findItem(R.id.cab_menu_send_to_chat).icon = Util.mutateIconSecondary(
            context, R.drawable.ic_send_to_contact,
            R.color.white
        )

        CloudStorageOptionControlUtil.applyControl(menu, control)
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val selectedNodes = viewModel.selectedNodes.value!!.map { it.node }
        val nodesHandles = ArrayList(selectedNodes.map { it!!.handle })

        if (item!!.itemId != R.id.cab_menu_select_all) {
            viewModel.clearSelection()   // include cab_menu_clear_selection
        }

        when (item.itemId) {
            R.id.cab_menu_download -> {
                NodeController(context).prepareForDownload(nodesHandles, false)
            }
            R.id.cab_menu_copy -> {
                NodeController(context).chooseLocationToCopyNodes(nodesHandles)
            }
            R.id.cab_menu_move -> {
                NodeController(context).chooseLocationToMoveNodes(nodesHandles)
            }
            R.id.cab_menu_share_out -> {
                MegaNodeUtil.shareNodes(context, selectedNodes)
            }
            R.id.cab_menu_share_link, R.id.cab_menu_edit_link -> {
                LogUtil.logDebug("Public link option")
                if (selectedNodes.size == 1) {
                    selectedNodes[0]?.let {
                        if (it.handle != MegaApiJava.INVALID_HANDLE) {
                            mainActivity.showGetLinkActivity(it.handle)
                        }
                    }
                }
            }
            R.id.cab_menu_remove_link -> {
                LogUtil.logDebug("Remove public link option")
                if (selectedNodes.size == 1) {
                    mainActivity.showConfirmationRemovePublicLink(selectedNodes[0])
                }
            }
            R.id.cab_menu_send_to_chat -> {
                LogUtil.logDebug("Send files to chat")
                NodeController(context).checkIfNodesAreMineAndSelectChatsToSendNodes(
                    ArrayList(selectedNodes)
                )
            }
            R.id.cab_menu_trash -> {
                mainActivity.askConfirmationMoveToRubbish(
                    nodesHandles
                )
            }
            R.id.cab_menu_select_all -> viewModel.trySelectAll()
        }

        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.let {
            val inflater = it.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
        }

        Log.i("Alex", "onCreateActionMode")

        mainActivity.apply {
            setDrawerLockMode(true)
            changeActionBarElevation(true)
        }

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.let {
            configItemsFun(menu)
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        viewModel.clearSelection()
        mainActivity.apply {
            changeActionBarElevation(false)
        }
    }

    fun setConfigItemsFun(function: (menu: Menu) -> Unit) {
        configItemsFun = function
    }
}