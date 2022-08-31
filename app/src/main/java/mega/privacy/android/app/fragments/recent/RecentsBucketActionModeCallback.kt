package mega.privacy.android.app.fragments.recent

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import nz.mega.sdk.MegaNode
import timber.log.Timber

/**
 * A action mode callback class for RecentsBucket
 */
class RecentsBucketActionModeCallback constructor(
    private val managerActivity: ManagerActivity,
    private val viewModel: RecentsBucketViewModel,
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater

        inflater.inflate(R.menu.recents_bucket_action, menu)

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onPrepareActionMode")

        menu!!.findItem(R.id.cab_menu_select_all).isVisible =
            (viewModel.getSelectedNodesCount() < viewModel.getNodesCount())

        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        Timber.d("ActionBarCallBack::onActionItemClicked")
        val selectedNodes = viewModel.getSelectedNodes().map { it.node }
        val selectedMegaNodes: List<MegaNode> = selectedNodes.mapNotNull { it }
        val nodesHandles: ArrayList<Long> = ArrayList(selectedMegaNodes.map { it.handle })
        when (item!!.itemId) {
            R.id.cab_menu_download -> {
                managerActivity.saveNodesToDevice(
                    selectedMegaNodes,
                    false,
                    false,
                    false,
                    false
                )
                viewModel.clearSelection()
            }
            R.id.cab_menu_share_link -> {
                LinksUtil.showGetLinkActivity(
                    managerActivity,
                    nodesHandles.toLongArray()
                )

                viewModel.clearSelection()
            }
            R.id.cab_menu_send_to_chat -> {
                managerActivity.attachNodesToChats(selectedMegaNodes)

                viewModel.clearSelection()
            }

            R.id.cab_menu_share_out -> {
                MegaNodeUtil.shareNodes(managerActivity, selectedMegaNodes)

                viewModel.clearSelection()
            }
            R.id.cab_menu_select_all -> {
                viewModel.selectAll()
            }
            R.id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
            }
            R.id.cab_menu_move -> {
                NodeController(managerActivity).chooseLocationToMoveNodes(nodesHandles)
                viewModel.clearSelection()
            }
            R.id.cab_menu_copy -> {
                NodeController(managerActivity).chooseLocationToCopyNodes(nodesHandles)
                viewModel.clearSelection()
            }
            R.id.cab_menu_trash -> {
                managerActivity.askConfirmationMoveToRubbish(nodesHandles)
                viewModel.clearSelection()
            }
        }

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        Timber.d("ActionBarCallBack::onDestroyActionMode")

        viewModel.clearSelection()
    }
}