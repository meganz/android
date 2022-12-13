package mega.privacy.android.app.presentation.photos.albums.actionMode

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotosFragment

class AlbumsActionModeCallback(
    private val fragment: PhotosFragment,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val inflater = mode?.menuInflater
        inflater?.inflate(R.menu.photos_albums_action, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_delete -> {}
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
}
