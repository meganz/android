package mega.privacy.android.app.mediaplayer

import android.animation.Animator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.exoplayer2.Player
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.SimpleAnimatorListener

/**
 * A view holder for audio player, implementing the UI logic of audio player.
 */
class AudioPlayerViewHolder(val binding: FragmentAudioPlayerBinding) {

    private val artworkContainer = binding.root.findViewById<CardView>(R.id.artwork_container)
    private val trackName = binding.root.findViewById<TextView>(R.id.track_name)
    private val artistName = binding.root.findViewById<TextView>(R.id.artist_name)
    private val playlist = binding.root.findViewById<ImageButton>(R.id.playlist)
    private val shuffle = binding.root.findViewById<ImageView>(R.id.exo_shuffle)

    /**
     * Update the layout param of artwork of player view.
     */
    fun layoutArtwork() {
        post {
            val resources = binding.root.resources

            val artworkWidth = resources.displayMetrics.widthPixels / 3 * 2
            val controllerHeight =
                resources.getDimensionPixelSize(R.dimen.audio_player_main_controller_height)

            val layoutParams = artworkContainer.layoutParams as FrameLayout.LayoutParams
            layoutParams.width = artworkWidth
            layoutParams.height = artworkWidth
            layoutParams.topMargin =
                (binding.playerView.measuredHeight - artworkWidth - controllerHeight) / 2
            artworkContainer.layoutParams = layoutParams

            artworkContainer.isVisible = true
        }
    }

    /**
     * Display node metadata.
     *
     * @param metadata metadata to display
     */
    fun displayMetadata(metadata: Metadata) {
        if (metadata.title != null && metadata.artist != null) {
            if (trackName.text.isEmpty()) {
                displayTrackAndArtist(trackName, artistName, metadata)
            } else {
                animateTrackAndArtist(trackName, false) {
                    displayTrackAndArtist(trackName, artistName, metadata)
                }

                if (artistName.isVisible) {
                    animateTrackAndArtist(artistName, false)
                }
            }
        } else {
            setTrackNameBottomMargin(trackName, false)
            val needAnimate = trackName.text != metadata.nodeName
            trackName.text = metadata.nodeName
            if (needAnimate) {
                animateTrackAndArtist(trackName, true)
            }

            artistName.isVisible = false
        }
    }

    private fun displayTrackAndArtist(
        trackName: TextView,
        artistName: TextView,
        metadata: Metadata
    ) {
        setTrackNameBottomMargin(trackName, true)
        trackName.text = metadata.title
        animateTrackAndArtist(trackName, true)

        artistName.isVisible = true
        artistName.text = metadata.artist
        animateTrackAndArtist(artistName, true)
    }

    private fun animateTrackAndArtist(
        textView: TextView,
        showing: Boolean,
        listener: (() -> Unit)? = null
    ) {
        textView.alpha = if (showing) 0F else 1F

        val animator = textView.animate()
        animator.cancel()

        if (listener != null) {
            animator.setListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator) {
                    animator.setListener(null)
                    listener()
                }
            })
        }

        animator
            .setDuration(Constants.AUDIO_PLAYER_TRACK_NAME_FADE_DURATION_MS)
            .alpha(if (showing) 1F else 0F)
            .start()
    }

    private fun setTrackNameBottomMargin(trackName: TextView, small: Boolean) {
        val params = trackName.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin = binding.root.resources.getDimensionPixelSize(
            if (small) R.dimen.audio_player_track_name_margin_bottom_small
            else R.dimen.audio_player_track_name_margin_bottom_large
        )
        trackName.layoutParams = params
    }

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
     * Toggle the playlist button.
     *
     * @param playlistItems the new playlist
     */
    fun togglePlaylistEnabled(playlistItems: List<PlaylistItem>) {
        playlist.isEnabled = playlistItems.size > MediaPlayerService.SINGLE_PLAYLIST_SIZE

        shuffle.isEnabled = playlist.isEnabled
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
    fun hideController() {
        binding.playerView.hideController()
    }
}
