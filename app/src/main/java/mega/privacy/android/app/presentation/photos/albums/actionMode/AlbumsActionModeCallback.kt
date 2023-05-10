package mega.privacy.android.app.presentation.photos.albums.actionMode

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.domain.entity.photos.Album

/**
 * Action Mode Callback class for Albums
 */
class AlbumsActionModeCallback(
    private val fragment: PhotosFragment,
    private val isAlbumSharingEnabled: Boolean,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.photos_albums_action, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.findItem(R.id.action_menu_get_link)?.let {
            it.isVisible = isAlbumSharingEnabled
            it.isEnabled = isAlbumSharingEnabled
            it.title = fragment.context?.resources?.getQuantityString(
                R.plurals.get_links, fragment.albumsViewModel.state.value.selectedAlbumIds.size
            )
        }

        menu?.findItem(R.id.action_menu_remove_link)?.let {
            it.isVisible = isAlbumSharingEnabled && fragment.isAllSelectedAlbumExported()
            it.isEnabled = isAlbumSharingEnabled && fragment.isAllSelectedAlbumExported()
        }
        updateSelectAllMenu(menu, fragment.albumsViewModel.state.value)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_menu_get_link -> {
                val selectedAlbums = fragment.albumsViewModel.state.value.selectedAlbumIds
                if (selectedAlbums.size == 1) {
                    fragment.openAlbumGetLinkScreen()
                } else {
                    fragment.openAlbumGetMultipleLinksScreen()
                }
                fragment.albumsViewModel.clearAlbumSelection()
            }
            R.id.action_delete -> {
                fragment.albumsViewModel.showDeleteAlbumsConfirmation()
            }
            R.id.action_menu_remove_link -> {
                fragment.albumsViewModel.showRemoveLinkDialog()
            }
            R.id.action_context_select_all -> {
                fragment.albumsViewModel.selectAllAlbums()
            }
            R.id.action_context_clear_selection -> {
                fragment.albumsViewModel.clearAlbumSelection()
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        fragment.albumsViewModel.clearAlbumSelection()
    }

    private fun updateSelectAllMenu(menu: Menu?, state: AlbumsViewState) {
        menu?.findItem(R.id.action_context_select_all)?.isVisible =
            state.selectedAlbumIds.size < state.albums.filter { it.id is Album.UserAlbum }.size
    }
}
