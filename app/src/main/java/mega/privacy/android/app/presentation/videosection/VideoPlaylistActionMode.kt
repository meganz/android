package mega.privacy.android.app.presentation.videosection

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.videosection.VideoSectionFragment.Companion.ACTION_TYPE_VIDEO_PLAYLIST_DETAIL

internal class VideoPlaylistActionMode(
    private val actionType: Int,
    private val managerActivity: ManagerActivity,
    private val videoSectionViewModel: VideoSectionViewModel,
    private val onActionModeFinished: () -> Unit,
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.video_playlist_action, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        item?.let {
            performItemOptionsClick(it)
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) = onActionModeFinished()

    private fun performItemOptionsClick(
        item: MenuItem,
    ) {
        managerActivity.lifecycleScope.launch {
            when (item.itemId) {
                R.id.select_all -> if (actionType == ACTION_TYPE_VIDEO_PLAYLIST_DETAIL) {
                    videoSectionViewModel.selectAllVideosOfPlaylist()
                } else {
                    videoSectionViewModel.selectAllVideoPlaylists()
                }

                R.id.clear_selection -> if (actionType == ACTION_TYPE_VIDEO_PLAYLIST_DETAIL) {
                    videoSectionViewModel.clearAllSelectedVideosOfPlaylist()
                } else {
                    videoSectionViewModel.clearAllSelectedVideoPlaylists()
                }


                R.id.action_delete -> if (actionType == ACTION_TYPE_VIDEO_PLAYLIST_DETAIL) {
                    videoSectionViewModel.setShouldDeleteVideosFromPlaylist(true)
                } else {
                    videoSectionViewModel.setShouldDeleteVideoPlaylist(true)
                }
            }
        }
    }
}