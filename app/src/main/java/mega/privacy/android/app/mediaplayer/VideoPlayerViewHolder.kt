package mega.privacy.android.app.mediaplayer

import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.exoplayer2.Player
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.databinding.FragmentVideoPlayerBinding

class VideoPlayerViewHolder(val binding: FragmentVideoPlayerBinding) {

    private val playlist = binding.root.findViewById<ImageButton>(R.id.playlist)
    private val trackName = binding.root.findViewById<TextView>(R.id.track_name)

    fun setupPlaylistButton(playlistItems: List<PlaylistItem>?, openPlaylist: () -> Unit) {
        if (playlistItems != null) {
            togglePlaylistEnabled(playlistItems)
        }

        playlist.setOnClickListener {
            openPlaylist()
        }
    }

    fun togglePlaylistEnabled(playlistItems: List<PlaylistItem>) {
        playlist.isVisible = playlistItems.size > MediaPlayerService.SINGLE_PLAYLIST_SIZE
    }

    fun updateLoadingAnimation(@Player.State playbackState: Int) {
        binding.loading.isVisible = playbackState == Player.STATE_BUFFERING
    }

    fun hideController() {
        binding.playerView.hideController()
    }

    fun displayMetadata(metadata: Metadata) {
        trackName.text = metadata.title ?: metadata.nodeName
    }
}
