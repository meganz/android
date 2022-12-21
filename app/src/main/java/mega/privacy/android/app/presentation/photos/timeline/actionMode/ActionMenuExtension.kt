package mega.privacy.android.app.presentation.photos.timeline.actionMode

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.clearSelectedPhotos
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.selectAllShowingPhotos
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeUtil

fun PhotosFragment.actionSaveToDevice() {
    lifecycleScope.launch {
        val selectedNodes = timelineViewModel.getSelectedNodes()
        managerActivity.saveNodesToDevice(selectedNodes,
            false,
            false,
            false,
            false)
    }
}

fun PhotosFragment.actionShareLink() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    try {
        LinksUtil.showGetLinkActivity(managerActivity, selectedPhotosIds.toLongArray())
    } catch (e: Exception) {
        e.printStackTrace() // workaround for potential risk if selectedPhotosIds is huge, need to refactor showGetLinkActivity
    }
}

fun PhotosFragment.actionSendToChat() {
    lifecycleScope.launch {
        val selectedNodes = timelineViewModel.getSelectedNodes()
        managerActivity.attachNodesToChats(selectedNodes)
    }
}

fun PhotosFragment.actionShareOut() {
    lifecycleScope.launch {
        val selectedNodes = timelineViewModel.getSelectedNodes()
        MegaNodeUtil.shareNodes(managerActivity, selectedNodes)
    }
}


fun PhotosFragment.actionSelectAll() {
    timelineViewModel.selectAllShowingPhotos()
}


fun PhotosFragment.actionClearSelection() {
    timelineViewModel.clearSelectedPhotos()
}

fun PhotosFragment.actionMove() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    NodeController(managerActivity).chooseLocationToMoveNodes(selectedPhotosIds)
}


fun PhotosFragment.actionCopy() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    NodeController(managerActivity).chooseLocationToCopyNodes(selectedPhotosIds)
}


fun PhotosFragment.actionMoveToTrash() {
    val selectedPhotosIds = arrayListOf<Long>().apply {
        addAll(timelineViewModel.getSelectedIds())
    }
    managerActivity.askConfirmationMoveToRubbish(
        selectedPhotosIds
    )
}

fun PhotosFragment.destroyActionMode() {
    timelineViewModel.clearSelectedPhotos()
}
