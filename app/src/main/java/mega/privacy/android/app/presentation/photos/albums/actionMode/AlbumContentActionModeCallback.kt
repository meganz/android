package mega.privacy.android.app.presentation.photos.albums.actionMode

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.AlbumDynamicContentFragment
import mega.privacy.android.domain.entity.photos.Album

class AlbumContentActionModeCallback(
    private val fragment: AlbumDynamicContentFragment,
    private val currentAlbum: Album?,
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
                fragment.actionSaveToDevice()
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
            R.id.cab_menu_remove_favourites -> {
                fragment.actionRemoveFavourites()
            }
            R.id.cab_menu_remove_photos -> {
                fragment.actionShowRemovePhotosFromAlbumDialog()
            }
        }
        return true
    }

    private fun handleActionItemVisibility(menu: Menu?) {
        menu?.let {
            if (currentAlbum != Album.FavouriteAlbum) {
                menu.findItem(R.id.cab_menu_remove_favourites)?.isVisible = false
            }
            if (currentAlbum !is Album.UserAlbum) {
                menu.findItem(R.id.cab_menu_remove_photos)?.isVisible = false
            }
            menu.findItem(R.id.cab_menu_select_all)?.isVisible = !fragment.checkSelectAll()
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        fragment.destroyActionMode()
    }
}