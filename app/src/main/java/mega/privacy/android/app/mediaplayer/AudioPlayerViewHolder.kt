package mega.privacy.android.app.mediaplayer

import android.animation.Animator
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentAudioPlayerBinding
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.queue.audio.AudioQueueFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.SimpleAnimatorListener
import mega.privacy.mobile.analytics.event.AudioPlayerQueueButtonPressedEvent

/**
 * A view holder for audio player, implementing the UI logic of audio player.
 *
 * @property binding [FragmentAudioPlayerBinding]
 */
class AudioPlayerViewHolder(val binding: FragmentAudioPlayerBinding) {
    private val artworkContainer = binding.root.findViewById<FrameLayout>(R.id.artwork_container)
    private val trackName = binding.root.findViewById<TextView>(R.id.track_name)
    private val artistName = binding.root.findViewById<TextView>(R.id.artist_name)
    private val playlist = binding.root.findViewById<ImageButton>(R.id.playlist)
    private val shuffle =
        binding.root.findViewById<ImageView>(androidx.media3.ui.R.id.exo_shuffle)
    internal val speedPlaybackButton = binding.root.findViewById<TextView>(R.id.speed_playback)
    internal val speedPlaybackPopup =
        binding.root.findViewById<ComposeView>(R.id.speed_playback_popup)

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
        metadata: Metadata,
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
        listener: (() -> Unit)? = null,
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
     * @param context Context
     * @param playlistItems the playlist
     * @param openPlaylist the callback when playlist button is clicked
     */
    fun setupPlaylistButton(
        context: Context,
        playlistItems: List<PlaylistItem>,
        shuffleEnabled: Boolean,
        openPlaylist: () -> Unit,
    ) {
        togglePlaylistEnabled(context, playlistItems, shuffleEnabled)

        playlist.setOnClickListener {
            Analytics.tracker.trackEvent(AudioPlayerQueueButtonPressedEvent)
            openPlaylist()
        }
    }

    /**
     * Toggle the playlist button.
     *
     * @param context Context
     * @param playlistItems the new playlist
     */
    fun togglePlaylistEnabled(
        context: Context,
        playlistItems: List<PlaylistItem>,
        shuffleEnabled: Boolean,
    ) {
        playlist.isEnabled = playlistItems.size > SINGLE_PLAYLIST_SIZE
        playlist.setColorFilter(
            context.getColor(
                if (playlist.isEnabled) {
                    R.color.dark_grey_white
                } else {
                    R.color.grey_050_grey_800
                }
            )
        )
        shuffle.isEnabled = playlist.isEnabled
        shuffle.setColorFilter(
            context.getColor(
                when {
                    !playlist.isEnabled ->
                        R.color.grey_050_grey_800

                    shuffleEnabled ->
                        R.color.color_button_brand

                    else ->
                        R.color.dark_grey_white
                }
            )
        )
    }

    /**
     * Update the visibility of loading animation.
     *
     * @param playbackState the state of player
     */
    fun updateLoadingAnimation(@Player.State playbackState: Int?) {
        binding.root.findViewById<View>(R.id.loading_audio_player_controller_view).isVisible =
            playbackState == Player.STATE_BUFFERING
        if (playbackState != null) {
            binding.root.findViewById<View>(R.id.play_pause_placeholder).visibility =
                if (playbackState > Player.STATE_BUFFERING) View.VISIBLE else View.INVISIBLE
        }
    }

    /**
     * Update the speed playback text according to the playback speed
     *
     * @param speedPlaybackItem SpeedPlaybackItem
     */
    fun updateSpeedPlaybackText(speedPlaybackItem: SpeedPlaybackItem) {
        speedPlaybackButton.text = speedPlaybackItem.text
    }
}
