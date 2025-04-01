package mega.privacy.android.app.presentation.photos.albums.albumcontent

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.mobile.analytics.event.AlbumContentHideNodeMenuItemEvent
import mega.privacy.mobile.analytics.event.AlbumContentRemoveItemsEvent

class AlbumContentActionModeCallback(
    private val fragment: AlbumContentFragment,
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.let {
            val inflater = it.menuInflater
            inflater.inflate(R.menu.photos_album_content_action, menu)
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        handleActionItemVisibility(menu)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.cab_menu_download -> {
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    saveToDevice()
                }
                clearSelection()
            }

            R.id.cab_menu_send_to_chat -> {
                sendToChat()
                clearSelection()
            }

            R.id.cab_menu_share_out -> {
                share()
                clearSelection()
            }

            R.id.cab_menu_select_all -> {
                selectAll()
                item.isVisible = false
            }

            R.id.cab_menu_clear_selection -> {
                clearSelection()
            }

            R.id.cab_menu_hide -> {
                Analytics.tracker.trackEvent(AlbumContentHideNodeMenuItemEvent)
                val state = fragment.albumContentViewModel.state.value
                val isPaid = state.accountType?.isPaid ?: false
                val isHiddenNodesOnboarded = state.isHiddenNodesOnboarded ?: false
                val isBusinessAccountExpired = state.isBusinessAccountExpired

                if (!isPaid || isBusinessAccountExpired) {
                    val intent = HiddenNodesOnboardingActivity.createScreen(
                        context = fragment.requireContext(),
                        isOnboarding = false,
                    )
                    fragment.hiddenNodesOnboardingLauncher.launch(intent)
                    fragment.activity?.overridePendingTransition(0, 0)
                } else if (isHiddenNodesOnboarded) {
                    fragment.albumContentViewModel.hideOrUnhideNodes(true)
                    clearSelection()
                } else {
                    fragment.albumContentViewModel.setHiddenNodesOnboarded()

                    val intent = HiddenNodesOnboardingActivity.createScreen(
                        context = fragment.requireContext(),
                        isOnboarding = true,
                    )
                    fragment.hiddenNodesOnboardingLauncher.launch(intent)
                    fragment.activity?.overridePendingTransition(0, 0)
                }
            }

            R.id.cab_menu_unhide -> {
                fragment.albumContentViewModel.hideOrUnhideNodes(false)
                clearSelection()
            }

            R.id.cab_menu_remove_favourites -> {
                removeFavourite()
            }

            R.id.cab_menu_remove_photos -> {
                Analytics.tracker.trackEvent(AlbumContentRemoveItemsEvent)
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    showRemoveDialog()
                }
            }
        }
        return true
    }

    private fun handleActionItemVisibility(menu: Menu?) {
        menu?.let {
            val album = fragment.albumContentViewModel.state.value.uiAlbum?.id
            if (album != Album.FavouriteAlbum) {
                menu.findItem(R.id.cab_menu_remove_favourites)?.isVisible = false
            }
            if (album !is Album.UserAlbum) {
                menu.findItem(R.id.cab_menu_remove_photos)?.isVisible = false
            }
            menu.findItem(R.id.cab_menu_select_all)?.isVisible = !isSelectAll()

            fragment.lifecycleScope.launch {
                val isHiddenNodesEnabled = isHiddenNodesActive()
                val selectedPhotos = fragment.albumContentViewModel.getSelectedPhotos()
                val includeSensitiveInheritedNode = selectedPhotos.any { it.isSensitiveInherited }

                if (isHiddenNodesEnabled) {
                    val hasNonSensitiveNode = selectedPhotos.any { !it.isSensitive }
                    val isPaid =
                        fragment.albumContentViewModel.state.value.accountType?.isPaid
                            ?: false

                    val isBusinessAccountExpired =
                        fragment.albumContentViewModel.state.value.isBusinessAccountExpired
                    val isHiddenNodesOnboarded =
                        fragment.albumContentViewModel.state.value.isHiddenNodesOnboarded

                    menu.findItem(R.id.cab_menu_hide)?.isVisible =
                        !isPaid || isBusinessAccountExpired || (hasNonSensitiveNode && isHiddenNodesOnboarded != null && !includeSensitiveInheritedNode)

                    menu.findItem(R.id.cab_menu_unhide)?.isVisible =
                        isPaid && !isBusinessAccountExpired && !hasNonSensitiveNode && !includeSensitiveInheritedNode
                } else {
                    menu.findItem(R.id.cab_menu_hide)?.isVisible = false
                    menu.findItem(R.id.cab_menu_unhide)?.isVisible = false
                }
            }
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        clearSelection()
    }

    private fun saveToDevice() {
        fragment.lifecycleScope.launch {
            val selectedNodes = fragment.albumContentViewModel.getSelectedNodes()
            fragment.managerActivity.saveNodesToDevice(
                selectedNodes,
                highPriority = false,
                isFolderLink = false,
                fromChat = false,
                withStartMessage = true,
            )
        }
    }

    private fun sendToChat() {
        fragment.lifecycleScope.launch {
            val selectedNodes = fragment.albumContentViewModel.getSelectedNodes()
            fragment.managerActivity.attachNodesToChats(selectedNodes)
        }
    }

    private fun share() {
        fragment.lifecycleScope.launch {
            val selectedNodes = fragment.albumContentViewModel.getSelectedNodes()
            MegaNodeUtil.shareNodes(fragment.managerActivity, selectedNodes)
        }
    }

    private fun selectAll() {
        fragment.albumContentViewModel.selectAllPhotos()
    }

    private fun clearSelection() {
        fragment.albumContentViewModel.clearSelectedPhotos()
    }

    private fun removeFavourite() {
        fragment.albumContentViewModel.removeFavourites()
    }

    private fun showRemoveDialog() {
        fragment.albumContentViewModel.setShowRemovePhotosFromAlbumDialog(true)
    }

    private fun isSelectAll(): Boolean {
        val selectedPhotos = fragment.albumContentViewModel.state.value.selectedPhotos
        val photos = fragment.albumContentViewModel.state.value.photos

        return selectedPhotos.size == photos.size
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            fragment.getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }
}