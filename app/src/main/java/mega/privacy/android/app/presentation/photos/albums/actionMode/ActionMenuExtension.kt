package mega.privacy.android.app.presentation.photos.albums.actionMode

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.photos.albums.AlbumDynamicContentFragment
import mega.privacy.android.app.utils.MegaNodeUtil

fun AlbumDynamicContentFragment.actionSaveToDevice() {
    lifecycleScope.launch {
        val selectedNodes = albumsViewModel.getSelectedNodes()
        managerActivity.saveNodesToDevice(selectedNodes,
            false,
            false,
            false,
            false)
    }
}

fun AlbumDynamicContentFragment.actionSendToChat() {
    lifecycleScope.launch {
        val selectedNodes = albumsViewModel.getSelectedNodes()
        managerActivity.attachNodesToChats(selectedNodes)
    }
}

fun AlbumDynamicContentFragment.actionShareOut() {
    lifecycleScope.launch {
        val selectedNodes = albumsViewModel.getSelectedNodes()
        MegaNodeUtil.shareNodes(managerActivity, selectedNodes)
    }
}

fun AlbumDynamicContentFragment.actionSelectAll() {
    albumsViewModel.selectAllPhotos()
}


fun AlbumDynamicContentFragment.actionClearSelection() {
    albumsViewModel.clearSelectedPhotos()
}


fun AlbumDynamicContentFragment.destroyActionMode() {
    albumsViewModel.clearSelectedPhotos()
}

fun AlbumDynamicContentFragment.actionRemoveFavourites() {
    albumsViewModel.removeFavourites()
}

fun AlbumDynamicContentFragment.checkSelectAll(): Boolean =
    albumsViewModel.selectedPhotoIds.size == albumsViewModel.getAlbumPhotosCount()


