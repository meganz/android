package mega.privacy.android.app.presentation.recentactions.recentactionbucket

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import timber.log.Timber

/**
 * A action mode callback class for [RecentActionBucketFragment]
 */
class RecentActionBucketActionModeCallback constructor(
    private val managerActivity: ManagerActivity,
    private val viewModel: RecentActionBucketViewModel,
    private val isInShareBucket: Boolean,
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater

        inflater.inflate(R.menu.recents_bucket_action, menu)

        val isAnyNodeInBackups = viewModel.isAnyNodeInBackups()

        menu?.let {
            it.findItem(R.id.cab_menu_share_link).isVisible = !isInShareBucket
            it.findItem(R.id.cab_menu_send_to_chat).isVisible = !isInShareBucket
            it.findItem(R.id.cab_menu_share_out).isVisible = !isInShareBucket
            // The "Move" and "Move to Rubbish Bin" Menu Options are automatically hidden
            // if at least one Node in the Selection belongs to Backups
            if (isInShareBucket || isAnyNodeInBackups) {
                it.findItem(R.id.cab_menu_move).isVisible = false
                it.findItem(R.id.cab_menu_trash).isVisible = false
            }
        }

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
        val selectedMegaNodes = viewModel.getSelectedMegaNodes()
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
