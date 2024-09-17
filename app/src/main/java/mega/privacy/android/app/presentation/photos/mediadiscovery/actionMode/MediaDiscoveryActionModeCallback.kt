package mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode

import mega.privacy.android.shared.resources.R as sharedR
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment

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
            val isHiddenNodesEnabled = fragment.getFeatureFlagUseCase(AppFeatures.HiddenNodes)
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

                menu?.findItem(R.id.cab_menu_hide)?.isVisible =
                    !isPaid || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
                menu?.findItem(R.id.cab_menu_unhide)?.isVisible =
                    isPaid && !hasNonSensitiveNode && !includeSensitiveInheritedNode
            } else {
                menu?.findItem(R.id.cab_menu_hide)?.isVisible = false
                menu?.findItem(R.id.cab_menu_unhide)?.isVisible = false
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
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        fragment.destroyActionMode()
    }
}