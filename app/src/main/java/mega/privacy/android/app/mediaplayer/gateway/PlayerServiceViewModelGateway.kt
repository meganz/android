package mega.privacy.android.app.mediaplayer.gateway

import android.content.Intent
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import java.io.File

/**
 * PlayerServiceViewModelGateway for visit MediaPlayerServiceViewModel from outside
 */
interface PlayerServiceViewModelGateway {
    /**
     * Remove item
     *
     * @param handle the handle that is removed
     */
    fun removeItem(handle: Long)

    /**
     * Set new text for playlist search query
     *
     * @param newText the new text string
     */
    fun searchQueryUpdate(newText: String?)

    /**
     * Get current intent
     *
     * @return current intent
     */
    fun getCurrentIntent(): Intent?

    /**
     * Get the handle of the current playing item
     *
     * @return the handle of the current playing item
     */
    fun getCurrentPlayingHandle(): Long

    /**
     *  Set the handle of the current playing item
     *
     *  @param handle MegaNode handle
     */
    fun setCurrentPlayingHandle(handle: Long)

    /**
     * Get playlist item
     *
     * @param handle MegaNode handle
     * @return PlaylistItem
     */
    fun getPlaylistItem(handle: String?): PlaylistItem?

    /**
     * Update when playlist is changed
     *
     * @return Flow<Pair<List<PlaylistItem>, Int>>
     */
    fun playlistUpdate(): Flow<Pair<List<PlaylistItem>, Int>>

    /**
     * Update item name
     *
     * @param handle MegaNode handle
     * @param newName the new name string
     */
    fun updateItemName(handle: Long, newName: String)

    /**
     * Get playlist items
     *
     * @return List<PlaylistItem>
     */
    fun getPlaylistItems(): List<PlaylistItem>

    /**
     * Update when media playback is changed
     *
     * @return Flow<Boolean>
     */
    fun mediaPlaybackUpdate(): Flow<Boolean>

    /**
     * Update when error is happened
     *
     * @return Flow<Int>
     */
    fun errorUpdate(): Flow<Int>

    /**
     * Update when playlist title is changed
     *
     * @return Flow<String>
     */
    fun playlistTitleUpdate(): Flow<String>

    /**
     * Update when item select count is changed
     *
     * @return Flow<Int>
     */
    fun itemsSelectedCountUpdate(): Flow<Int>

    /**
     * Update when action mode is changed
     *
     * @return Flow<Boolean>
     */
    fun actionModeUpdate(): Flow<Boolean>

    /**
     * Remove the selected items
     */
    fun removeAllSelectedItems()

    /**
     * Clear the all selections
     */
    fun clearSelections()

    /**
     * Scroll to the position of playing item
     */
    fun scrollToPlayingPosition()


    /**
     * Judge the action mode whether is enabled
     *
     * @return true is action mode, otherwise is false.
     */
    fun isActionMode(): Boolean?

    /**
     * Set the action mode
     * @param isActionMode whether the action mode is activated
     */
    fun setActionMode(isActionMode: Boolean)

    /**
     * Saved or remove the selected items
     * @param handle node handle of selected item
     */
    fun itemSelected(handle: Long)

    /**
     * Get the index from playlistItems to keep the play order is correct after reordered
     * @param item clicked item
     * @return the index of clicked item in playlistItems or null
     */
    fun getIndexFromPlaylistItems(item: PlaylistItem): Int?

    /**
     * Get the position of playing item
     *
     * @return the position of playing item
     */
    fun getPlayingPosition(): Int

    /**
     * Swap the items
     * @param current the position of from item
     * @param target the position of to item
     */
    fun swapItems(current: Int, target: Int)

    /**
     * Updated the play source of exoplayer after reordered.
     */
    fun updatePlaySource()

    /**
     * Judge the current media item whether is paused
     *
     * @return true is paused, otherwise is false
     */
    fun isPaused(): Boolean

    /**
     * Set paused
     * @param paused the paused state
     */
    fun setPaused(paused: Boolean)

    /**
     * Handle player error.
     */
    fun onPlayerError()

    /**
     * Get playing thumbnail
     *
     * @return LiveData<File>
     */
    fun getPlayingThumbnail(): LiveData<File>

    /**
     * Update playerSource
     *
     * @return Flow<MediaPlaySources>
     */
    fun playerSourceUpdate(): Flow<MediaPlaySources>

    /**
     * Update when item is removed
     *
     * @return Flow<Int>
     */
    fun mediaItemToRemoveUpdate(): Flow<Int>

    /**
     * Update node name
     *
     * @return Flow<String>
     */
    fun nodeNameUpdate(): Flow<String>

    /**
     * Update retry
     *
     * @return Flow<Boolean>
     */
    fun retryUpdate(): Flow<Boolean>

    /**
     * Build player source from start intent.
     *
     * @param intent intent received from onStartCommand
     * @return if there is no error
     */
    suspend fun buildPlayerSource(intent: Intent?): Boolean

    /**
     * Cancel search token
     */
    fun cancelSearch()

    /**
     * Clear the state and flying task of this class, should be called in onDestroy.
     */
    fun clear()

    /**
     * Reset retry state
     */
    fun resetRetryState()
}