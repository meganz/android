package mega.privacy.android.app.presentation.photos.timeline.actionMode

import mega.privacy.android.shared.resources.R as sharedR
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.PhotosFragment

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
            val isHiddenNodesEnabled = fragment.getFeatureFlagUseCase(AppFeatures.HiddenNodes)
            val hasNonSensitiveNode =
                fragment.timelineViewModel.getSelectedNodes().any { !it.isMarkedSensitive }

            menu?.findItem(R.id.cab_menu_share_link)?.let {
                it.title = fragment.context?.resources?.getQuantityString(
                    sharedR.plurals.label_share_links,
                    fragment.timelineViewModel.state.value.selectedPhotoCount
                )
            }
            menu?.findItem(R.id.cab_menu_hide)?.isVisible =
                isHiddenNodesEnabled && hasNonSensitiveNode

            menu?.findItem(R.id.cab_menu_unhide)?.isVisible =
                isHiddenNodesEnabled && !hasNonSensitiveNode
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
                fragment.handleHideNodeClick()
                fragment.destroyActionMode()
            }

            R.id.cab_menu_unhide -> {
                fragment.timelineViewModel.hideOrUnhideNodes(hide = false)
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