package mega.privacy.android.app.presentation.photos.mediadiscovery.actionMode

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import timber.log.Timber.Forest.e

fun MediaDiscoveryFragment.actionSaveToDevice() {
    lifecycleScope.launch {
        val selectedNodes = mediaDiscoveryViewModel.getSelectedNodes()
        managerActivity.saveNodesToDevice(selectedNodes,
            false,
            false,
            false,
            false)
    }
}

fun MediaDiscoveryFragment.actionShareLink() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(mediaDiscoveryViewModel.getSelectedIds())
    }
    try {
        LinksUtil.showGetLinkActivity(managerActivity, selectedPhotosIds.toLongArray())
    } catch (e: Exception) {
        e(e) // workaround for potential risk if selectedPhotosIds is huge, need to refactor showGetLinkActivity
    }
}

fun MediaDiscoveryFragment.actionSendToChat() {
    lifecycleScope.launch {
        val selectedNodes = mediaDiscoveryViewModel.getSelectedNodes()
        managerActivity.attachNodesToChats(selectedNodes)
    }
}

fun MediaDiscoveryFragment.actionShareOut() {
    lifecycleScope.launch {
        val selectedNodes = mediaDiscoveryViewModel.getSelectedNodes()
        MegaNodeUtil.shareNodes(managerActivity, selectedNodes)
    }
}


fun MediaDiscoveryFragment.actionSelectAll() {
    mediaDiscoveryViewModel.selectAllPhotos()
}


fun MediaDiscoveryFragment.actionClearSelection() {
    mediaDiscoveryViewModel.clearSelectedPhotos()
}

fun MediaDiscoveryFragment.actionMove() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(mediaDiscoveryViewModel.getSelectedIds())
    }
    NodeController(managerActivity).chooseLocationToMoveNodes(selectedPhotosIds)
}


fun MediaDiscoveryFragment.actionCopy() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(mediaDiscoveryViewModel.getSelectedIds())
    }
    NodeController(managerActivity).chooseLocationToCopyNodes(selectedPhotosIds)
}


fun MediaDiscoveryFragment.actionMoveToTrash() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(mediaDiscoveryViewModel.getSelectedIds())
    }
    managerActivity.askConfirmationMoveToRubbish(
        selectedPhotosIds
    )
}

fun MediaDiscoveryFragment.destroyActionMode() {
    mediaDiscoveryViewModel.clearSelectedPhotos()
}
