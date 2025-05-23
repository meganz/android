package mega.privacy.android.app.presentation.photos.timeline.actionMode

import mega.privacy.android.shared.resources.R as sharedR
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.mobile.analytics.event.TimelineHideNodeMenuItemEvent

class TimelineActionModeCallback(
    private val fragment: PhotosFragment,
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.let {
            val inflater = it.menuInflater
            inflater.inflate(R.menu.photos_timeline_action, menu)
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        fragment.lifecycleScope.launch {
            val isHiddenNodesEnabled = isHiddenNodesActive()
            val selectedNodes = fragment.timelineViewModel.getSelectedTypedNodes()
            val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }
            menu?.findItem(R.id.cab_menu_share_link)?.let {
                it.title = fragment.context?.resources?.getQuantityString(
                    sharedR.plurals.label_share_links,
                    selectedNodes.size
                )
            }

            menu?.findItem(R.id.cab_menu_remove_link)?.let {
                it.isVisible =
                    selectedNodes.size == 1 && fragment.timelineViewModel.getSelectedNodes()
                        .firstOrNull()?.isExported ?: false
            }

            if (isHiddenNodesEnabled) {
                val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }

                val isPaid =
                    fragment.timelineViewModel.state.value.accountType?.isPaid
                        ?: false

                val isBusinessAccountExpired =
                    fragment.timelineViewModel.state.value.isBusinessAccountExpired

                menu?.findItem(R.id.cab_menu_hide)?.isVisible =
                    !isPaid || isBusinessAccountExpired || hasNonSensitiveNode && !includeSensitiveInheritedNode

                menu?.findItem(R.id.cab_menu_unhide)?.isVisible =
                    isPaid && !isBusinessAccountExpired && !hasNonSensitiveNode && !includeSensitiveInheritedNode
            } else {
                menu?.findItem(R.id.cab_menu_hide)?.isVisible = false
                menu?.findItem(R.id.cab_menu_unhide)?.isVisible = false
            }

            menu?.findItem(R.id.cab_menu_add_to_album)?.let {
                it.isVisible = selectedNodes
                    .filter { node ->
                        val type = (node as? FileNode)?.type
                        type is ImageFileTypeInfo || type is VideoFileTypeInfo
                    }.size == selectedNodes.size
            }
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.cab_menu_download -> {
                fragment.actionSaveToDevice()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_share_link -> {
                fragment.actionShareLink()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_send_to_chat -> {
                fragment.actionSendToChat()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_share_out -> {
                fragment.actionShareOut()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_select_all -> {
                fragment.actionSelectAll()
            }

            R.id.cab_menu_clear_selection -> {
                fragment.actionClearSelection()
            }

            R.id.cab_menu_hide -> {
                Analytics.tracker.trackEvent(TimelineHideNodeMenuItemEvent)
                fragment.handleHideNodeClick()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_unhide -> {
                val handles = fragment.timelineViewModel.selectedPhotosIds.toList()
                fragment.timelineViewModel.hideOrUnhideNodes(
                    hide = false,
                    handles = handles,
                )
                val message = fragment.resources.getQuantityString(
                    sharedR.plurals.unhidden_nodes_result_message,
                    handles.size,
                    handles.size
                )
                Util.showSnackbar(fragment.requireActivity(), message)
                fragment.destroyActionMode()
            }

            R.id.cab_menu_move -> {
                fragment.actionMove()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_copy -> {
                fragment.actionCopy()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_trash -> {
                fragment.actionMoveToTrash()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_remove_link -> {
                fragment.actionRemoveLink()
                fragment.destroyActionMode()

            }

            R.id.cab_menu_add_to_album -> {
                val nodeIds = fragment.timelineViewModel.selectedPhotosIds.map { NodeId(it) }
                fragment.openAddToAlbum(nodeIds, 0)
                fragment.destroyActionMode()
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        fragment.destroyActionMode()
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            fragment.getFeatureFlagUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }
}