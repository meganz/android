package mega.privacy.android.app.mediaplayer.playlist

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility

/**
 * Action mode callback for playlist fragment
 *
 * @property removeSelections the function for removing all selections
 * @property clearSelections the function for clearing all selections
 */
class PlaylistActionModeCallback(
    private val removeSelections: () -> Unit,
    private val clearSelections: () -> Unit,
) :
    ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.media_player, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        // When the action mode is initialized, set the visibilities of all menu item are false
        menu?.toggleAllMenuItemsVisibility(false)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (item?.itemId == R.id.remove) {
            removeSelections
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // When the action mode is finished, clear the selections
        clearSelections
    }
}