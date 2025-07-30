package mega.privacy.android.app.presentation.recentactions.recentactionbucket

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.isGif
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/**
 * A action mode callback class for [RecentActionBucketFragment]
 */
class RecentActionBucketActionModeCallback constructor(
    private val managerActivity: ManagerActivity,
    private val recentActionBucketFragment: RecentActionBucketFragment,
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

        menu?.let {
            it.findItem(R.id.cab_menu_select_all).isVisible =
                (viewModel.getSelectedNodesCount() < viewModel.getNodesCount())

            it.findItem(R.id.cab_menu_share_link)?.let { item ->
                item.title = recentActionBucketFragment.context?.resources?.getQuantityString(
                    sharedR.plurals.label_share_links,
                    viewModel.getSelectedNodesCount()
                )
            }

            handleHiddenNodes(it)
            handleAddToAlbums(it)
        }

        return true
    }

    private fun handleHiddenNodes(menu: Menu) {
        managerActivity.lifecycleScope.launch {
            val isHiddenNodesEnabled = isHiddenNodesActive()
            val selectedNodes = viewModel.getSelectedNodes()
            val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }

            if (isHiddenNodesEnabled && !isInShareBucket) {
                val accountType = viewModel.state.value.accountType
                val isPaid = accountType?.isPaid ?: false
                val isBusinessAccountExpired = viewModel.state.value.isBusinessAccountExpired

                val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }
                menu.findItem(R.id.cab_menu_hide)?.isVisible =
                    !isPaid || isBusinessAccountExpired || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
                menu.findItem(R.id.cab_menu_unhide)?.isVisible =
                    isPaid && !isBusinessAccountExpired && !hasNonSensitiveNode && !includeSensitiveInheritedNode
            }
        }
    }

    private fun handleAddToAlbums(menu: Menu) {
        val selectedNodes = viewModel.getSelectedNodes()
        val mediaNodes = selectedNodes
            .mapNotNull { it.node }
            .filter { it.isImage() || it.isGif() || it.isVideo() }

        if (mediaNodes.size == selectedNodes.size) {
            if (mediaNodes.all { it.isVideo() }) {
                menu.findItem(R.id.cab_menu_add_to_album)?.isVisible = false
                menu.findItem(R.id.cab_menu_add_to)?.isVisible = true
            } else {
                menu.findItem(R.id.cab_menu_add_to_album)?.isVisible = true
                menu.findItem(R.id.cab_menu_add_to)?.isVisible = false
            }
        } else {
            menu.findItem(R.id.cab_menu_add_to_album)?.isVisible = false
            menu.findItem(R.id.cab_menu_add_to)?.isVisible = false
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        Timber.d("ActionBarCallBack::onActionItemClicked")
        val selectedMegaNodes = viewModel.getSelectedMegaNodes()
        val nodesHandles = selectedMegaNodes.map { it.handle }
        val nodeIds = nodesHandles.map { NodeId(it) }
        when (item!!.itemId) {
            R.id.cab_menu_download -> {
                managerActivity.saveNodesToDevice(
                    nodes = selectedMegaNodes,
                    highPriority = false,
                    isFolderLink = false,
                    fromChat = false,
                    withStartMessage = false,
                )
                viewModel.clearSelection()
            }

            R.id.cab_menu_share_link -> {
                if (selectedMegaNodes.size == 1) {
                    LinksUtil.showGetLinkActivity(
                        managerActivity,
                        nodesHandles[0]
                    )
                } else {
                    LinksUtil.showGetLinkActivity(
                        managerActivity,
                        nodesHandles.toLongArray()
                    )
                }
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

            R.id.cab_menu_hide -> {
                recentActionBucketFragment.onHideClicked(nodeIds = nodeIds)
                viewModel.clearSelection()
            }

            R.id.cab_menu_unhide -> {
                viewModel.hideOrUnhideNodes(nodeIds = nodeIds, false)
                val message = recentActionBucketFragment.resources.getQuantityString(
                    R.plurals.hidden_nodes_result_message,
                    nodeIds.size,
                    nodeIds.size
                )
                Util.showSnackbar(recentActionBucketFragment.requireActivity(), message)
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

            R.id.cab_menu_add_to_album -> {
                val intent = Intent(managerActivity, AddToAlbumActivity::class.java).apply {
                    val ids = nodeIds.map { it.longValue }.toTypedArray()
                    putExtra("ids", ids)
                    putExtra("type", 0)
                }
                recentActionBucketFragment.addToAlbumLauncher.launch(intent)
                viewModel.clearSelection()
            }

            R.id.cab_menu_add_to -> {
                val intent = Intent(managerActivity, AddToAlbumActivity::class.java).apply {
                    val ids = nodeIds.map { it.longValue }.toTypedArray()
                    putExtra("ids", ids)
                    putExtra("type", 1)
                }
                recentActionBucketFragment.addToAlbumLauncher.launch(intent)
                viewModel.clearSelection()
            }

            R.id.cab_menu_trash -> {
                if (nodesHandles.isNotEmpty()) {
                    ConfirmMoveToRubbishBinDialogFragment.newInstance(nodesHandles)
                        .show(
                            managerActivity.supportFragmentManager,
                            ConfirmMoveToRubbishBinDialogFragment.TAG
                        )
                }
                viewModel.clearSelection()
            }
        }

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        Timber.d("ActionBarCallBack::onDestroyActionMode")

        viewModel.clearSelection()
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            managerActivity.getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }
}
