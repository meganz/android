package mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode

import mega.privacy.android.shared.resources.R as sharedR
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode

class MediaDiscoveryActionModeCallback(
    val fragment: MediaDiscoveryFragment,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.let {
            val inflater = it.menuInflater
            inflater.inflate(R.menu.media_discovery_action, menu)
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        fragment.lifecycleScope.launch {
            val isHiddenNodesEnabled = isHiddenNodesActive()
            val selectedNodes = fragment.mediaDiscoveryViewModel.getSelectedTypedNodes()
            val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }
            menu?.findItem(R.id.cab_menu_share_link)?.let {
                it.title = fragment.context?.resources?.getQuantityString(
                    sharedR.plurals.label_share_links, selectedNodes.size
                )
            }

            if (isHiddenNodesEnabled) {
                val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }

                val isPaid =
                    fragment.mediaDiscoveryViewModel.state.value.accountType?.isPaid
                        ?: false

                val isBusinessAccountExpired =
                    fragment.mediaDiscoveryViewModel.state.value.isBusinessAccountExpired

                menu?.findItem(R.id.cab_menu_hide)?.isVisible =
                    !isPaid || isBusinessAccountExpired || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
                menu?.findItem(R.id.cab_menu_unhide)?.isVisible =
                    isPaid && !isBusinessAccountExpired && !hasNonSensitiveNode && !includeSensitiveInheritedNode
            } else {
                menu?.findItem(R.id.cab_menu_hide)?.isVisible = false
                menu?.findItem(R.id.cab_menu_unhide)?.isVisible = false
            }

            val mediaNodes = selectedNodes
                .filter {
                    val type = (it as? FileNode)?.type
                    type is ImageFileTypeInfo || type is VideoFileTypeInfo
                }
            if (mediaNodes.size == selectedNodes.size) {
                if (mediaNodes.all { (it as? FileNode)?.type is VideoFileTypeInfo }) {
                    menu?.findItem(R.id.cab_menu_add_to_album)?.isVisible = false
                    menu?.findItem(R.id.cab_menu_add_to)?.isVisible = true
                } else {
                    menu?.findItem(R.id.cab_menu_add_to_album)?.isVisible = true
                    menu?.findItem(R.id.cab_menu_add_to)?.isVisible = false
                }
            } else {
                menu?.findItem(R.id.cab_menu_add_to_album)?.isVisible = false
                menu?.findItem(R.id.cab_menu_add_to)?.isVisible = false
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
                item.isVisible = false
            }

            R.id.cab_menu_clear_selection -> {
                fragment.actionClearSelection()
            }

            R.id.cab_menu_hide -> {
                fragment.handleHideNodeClick()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_unhide -> {
                fragment.mediaDiscoveryViewModel.hideOrUnhideNodes(hide = false)
                val size = fragment.mediaDiscoveryViewModel.getSelectedIds().size
                val message = fragment.resources.getQuantityString(
                    sharedR.plurals.unhidden_nodes_result_message,
                    size,
                    size,
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

            R.id.cab_menu_add_to_album -> with(fragment) {
                val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                    val ids = mediaDiscoveryViewModel.state.value.selectedPhotoIds.toTypedArray()
                    putExtra("ids", ids)
                    putExtra("type", 0)
                }
                addToAlbumLauncher.launch(intent)
                destroyActionMode()
            }

            R.id.cab_menu_add_to -> with(fragment) {
                val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                    val ids = mediaDiscoveryViewModel.state.value.selectedPhotoIds.toTypedArray()
                    putExtra("ids", ids)
                    putExtra("type", 1)
                }
                addToAlbumLauncher.launch(intent)
                destroyActionMode()
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