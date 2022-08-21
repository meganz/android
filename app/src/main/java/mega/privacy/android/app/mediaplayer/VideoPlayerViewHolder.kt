package mega.privacy.android.app.mediaplayer

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.exoplayer2.Player
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.Metadata

/**
 * A view holder for video player, implementing the UI logic of video player.
 *
 * @property binding FragmentVideoPlayerBinding
 */
class VideoPlayerViewHolder(val binding: FragmentVideoPlayerBinding) {

    private val playlist = binding.root.findViewById<ImageButton>(R.id.playlist)
    private val trackName = binding.root.findViewById<TextView>(R.id.track_name)
    private val repeatToggleButton = binding.root.findViewById<ImageButton>(R.id.repeat_toggle)

    /**
     * Setup playlist button.
     *
     * @param playlistItems the playlist
     * @param openPlaylist the callback when playlist button is clicked
     */
    fun setupPlaylistButton(playlistItems: List<PlaylistItem>?, openPlaylist: () -> Unit) {
        if (playlistItems != null) {
            togglePlaylistEnabled(playlistItems)
        }

        playlist.setOnClickListener {
            openPlaylist()
        }
    }

    /**
     * Setup the repeat toggle button
     *
     * @param defaultRepeatToggleMode the default RepeatToggleMode
     * @param clickedCallback the callback of repeat toggle button clicked
     */
    fun setupRepeatToggleButton(
        defaultRepeatToggleMode: RepeatToggleMode,
        clickedCallback: (repeatToggleButton: ImageButton) -> Unit
    ) {
        with(repeatToggleButton) {
            isVisible = true
            setImageResource(
                if (defaultRepeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                    R.drawable.exo_controls_repeat_off
                } else {
                    R.drawable.exo_controls_repeat_all
                }
            )
            setOnClickListener {
                clickedCallback(this)
            }
        }
    }

    /**
     * Toggle the playlist button.
     *
     * @param playlistItems the new playlist
     */
    fun togglePlaylistEnabled(playlistItems: List<PlaylistItem>) {
        if (playlistItems.size > MediaPlayerService.SINGLE_PLAYLIST_SIZE) {
            playlist.visibility = View.VISIBLE

            binding.playerView.setShowNextButton(true)
        } else {
            playlist.visibility = View.INVISIBLE

            binding.playerView.setShowNextButton(false)
        }
    }

    /**
     * Update the visibility of loading animation.
     *
     * @param playbackState the state of player
     */
    fun updateLoadingAnimation(@Player.State playbackState: Int) {
        binding.loading.isVisible = playbackState == Player.STATE_BUFFERING
    }

    /**
     * Hide player controller.
     */
    fun hideController() = binding.playerView.hideController()

    /**
     * Show player controller.
     */
    fun showController() = binding.playerView.showController()

    /**
     * Display node metadata.
     *
     * @param metadata metadata to display
     */
    fun displayMetadata(metadata: Metadata) {
        trackName.text = metadata.title ?: metadata.nodeName
    }
}
