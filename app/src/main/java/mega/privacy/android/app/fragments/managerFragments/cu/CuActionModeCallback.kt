package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveOwnerAccess
import mega.privacy.android.app.utils.MegaNodeUtil.canMoveToRubbish
import mega.privacy.android.app.utils.MegaNodeUtil.shareNodes
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import java.util.*

class CuActionModeCallback(
    private val mContext: Context,
    private val mFragment: PhotosFragment,
    private val mViewModel: CuViewModel,
    private val mMegaApi: MegaApiAndroid
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        LogUtil.logDebug("onCreateActionMode")
        val inflater = mode!!.menuInflater
        inflater.inflate(R.menu.cloud_storage_action, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu): Boolean {
        LogUtil.logDebug("onPrepareActionMode")
        val selected: List<MegaNode?>? = mViewModel.getSelectedNodes()
        if (selected!!.isEmpty()) {
            return false
        }

        menu.findItem(R.id.cab_menu_share_link).title =
            StringResourcesUtils.getQuantityString(R.plurals.get_links, selected.size)

        val control = CloudStorageOptionControlUtil.Control()

        if (selected.size == 1
            && mMegaApi.checkAccess(selected[0], MegaShare.ACCESS_OWNER).errorCode
            == MegaError.API_OK
        ) {
            if (selected[0]!!.isExported) {
                control.manageLink().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                control.removeLink().isVisible = true
            } else {
                control.link.setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            }
        } else if (allHaveOwnerAccess(selected)) {
            control.link.setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        }

        control.sendToChat().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS

        control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS

        control.trash().isVisible = canMoveToRubbish(selected)

        control.move().isVisible = true
        control.copy().isVisible = true
        if (selected.size > 1 && !control.link.isVisible) {
            control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        }

        control.selectAll().isVisible = selected.size < mViewModel.getRealNodesCount()

        CloudStorageOptionControlUtil.applyControl(menu, control)

        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        LogUtil.logDebug("onActionItemClicked")
        val documents = mViewModel.getSelectedNodes()
        if (documents!!.isEmpty()) {
            return false
        }

        when (item.itemId) {
            R.id.cab_menu_download -> {
                mViewModel.clearSelection()
                (mContext as ManagerActivityLollipop)
                    .saveNodesToDevice(documents, false, false, false, false)
            }
            R.id.cab_menu_copy -> {
                mViewModel.clearSelection()
                NodeController(mContext)
                    .chooseLocationToCopyNodes(getDocumentHandles(documents))
            }
            R.id.cab_menu_move -> {
                mViewModel.clearSelection()
                NodeController(mContext)
                    .chooseLocationToMoveNodes(getDocumentHandles(documents))
            }
            R.id.cab_menu_share_out -> {
                mViewModel.clearSelection()
                shareNodes(mContext, documents)
            }
            R.id.cab_menu_share_link, R.id.cab_menu_edit_link -> {
                LogUtil.logDebug("Public link option")
                mViewModel.clearSelection()
                (mContext as ManagerActivityLollipop).showGetLinkActivity(documents)
            }
            R.id.cab_menu_remove_link -> {
                LogUtil.logDebug("Remove public link option")
                mViewModel.clearSelection()
                if (documents.size == 1) {
                    (mContext as ManagerActivityLollipop)
                        .showConfirmationRemovePublicLink(documents[0])
                }
            }
            R.id.cab_menu_send_to_chat -> {
                LogUtil.logDebug("Send files to chat")
                (mContext as ManagerActivityLollipop).attachNodesToChats(documents)
                mViewModel.clearSelection()
            }
            R.id.cab_menu_trash -> {
                mViewModel.clearSelection()
                (mContext as ManagerActivityLollipop).askConfirmationMoveToRubbish(
                    getDocumentHandles(documents)
                )
            }
            R.id.cab_menu_select_all -> mFragment.selectAll()
            R.id.cab_menu_clear_selection -> mViewModel.clearSelection()
        }
        return true
    }

    /**
     * Get handles for selected nodes.
     *
     * @return handles for selected nodes.
     */
    private fun getDocumentHandles(documents: List<MegaNode>): ArrayList<Long> {
        val handles = ArrayList<Long>()
        for (node in documents) {
            handles.add(node.handle)
        }
        return handles
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        LogUtil.logDebug("onDestroyActionMode")
        mViewModel.clearSelection()
    }
}