package mega.privacy.android.app.mediaplayer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.playlist.AudioPlaylistFragment.Companion.SINGLE_PLAYLIST_SIZE
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * A view holder for video player, implementing the UI logic of video player.
 *
 * @property container view for VideoPlayerFragment
 */
class VideoPlayerViewHolder(val container: ViewGroup) {

    private val playlist = container.findViewById<ImageButton>(R.id.playlist)
    private val trackName = container.findViewById<TextView>(R.id.track_name)
    private val repeatToggleButton = container.findViewById<ImageButton>(R.id.repeat_toggle)
    private val lockButton = container.findViewById<ImageButton>(R.id.image_button_lock)
    private val unlockButton = container.findViewById<ImageButton>(R.id.image_button_unlock)
    private val playerLayout = container.findViewById<ConstraintLayout>(R.id.layout_player)
    private val unlockLayout = container.findViewById<ConstraintLayout>(R.id.layout_unlock)
    private val screenshotButton = container.findViewById<ImageButton>(R.id.image_screenshot)
    private val subtitleButton = container.findViewById<ImageButton>(R.id.subtitle)
    private val fullScreenButton = container.findViewById<ImageButton>(R.id.full_screen)
    internal val speedPlaybackButton = container.findViewById<ImageButton>(R.id.speed_playback)
    internal val moreOptionButton = container.findViewById<ImageButton>(R.id.more_option)
    internal val playerView = container.findViewById<PlayerView>(R.id.player_view)
    internal val speedPlaybackPopup = container.findViewById<ComposeView>(R.id.speed_playback_popup)
    internal val videoOptionPopup = container.findViewById<ComposeView>(R.id.video_option_popup)

    /**
     * Setup playlist button.
     *
     * @param playlistItems the playlist
     * @param openPlaylist the callback when playlist button is clicked
     */
    fun setupPlaylistButton(playlistItems: List<PlaylistItem>, openPlaylist: () -> Unit) {
        togglePlaylistEnabled(playlistItems)

        playlist.setOnClickListener {
            openPlaylist()
        }
    }

    /**
     * Setup the screenshot button
     *
     * @param clickedCallback the callback of screenshot button clicked
     */
    fun setupScreenshotButton(clickedCallback: () -> Unit) {
        // The screenshot feature is not available if the Android version is lower that API 26
        screenshotButton.setOnClickListener {
            clickedCallback()
        }
    }

    /**
     * Setup the repeat toggle button
     *
     * @param context Context
     * @param defaultRepeatToggleMode the default RepeatToggleMode
     * @param clickedCallback the callback of repeat toggle button clicked
     */
    fun setupRepeatToggleButton(
        context: Context,
        defaultRepeatToggleMode: RepeatToggleMode,
        clickedCallback: () -> Unit,
    ) {
        repeatToggleButton.isVisible = true
        updateRepeatToggleButtonUI(context, defaultRepeatToggleMode)
        repeatToggleButton.setOnClickListener { clickedCallback() }
    }

    /**
     * Update repeat toggle button UI
     *
     * @param context Context
     * @param repeatToggleMode the current RepeatToggleMode
     */
    fun updateRepeatToggleButtonUI(
        context: Context,
        repeatToggleMode: RepeatToggleMode,
    ) {
        repeatToggleButton.setColorFilter(
            if (repeatToggleMode == RepeatToggleMode.REPEAT_NONE) {
                context.getColor(R.color.white)
            } else {
                context.getColor(R.color.teal_300)
            }
        )
    }

    /**
     * Setup the UI based on the lock feature
     *
     * @param defaultLockStatus the default lock status
     * @param lockCallback the callback regarding lock feature
     */
    fun setupLockUI(defaultLockStatus: Boolean, lockCallback: (isLock: Boolean) -> Unit) {
        updateLockUI(defaultLockStatus)
        lockButton.setOnClickListener {
            updateLockUI(true)
            lockCallback(true)
        }
        unlockButton.setOnClickListener {
            updateLockUI(false)
            lockCallback(false)
        }
    }

    /**
     * Update the lock UI
     *
     * @param isLock true is shown the lock UI, otherwise is false
     */
    fun updateLockUI(isLock: Boolean) {
        playerLayout.isVisible = !isLock
        unlockLayout.isVisible = isLock
    }

    /**
     * Update the speed playback icon according to the playback speed
     *
     * @param iconId icon resources id
     */
    fun updateSpeedPlaybackIcon(@DrawableRes iconId: Int) {
        speedPlaybackButton.setImageResource(iconId)
    }

    /**
     * Setup the full screen button
     *
     * @param isFullScreen for update the full screen button UI
     * @param fullscreenCallback the callback for the full screen button is clicked
     */
    fun setupFullScreen(isFullScreen: Boolean, fullscreenCallback: () -> Unit) {
        updateFullScreenUI(isFullScreen)
        fullScreenButton.setOnClickListener {
            fullscreenCallback()
        }
    }

    /**
     * Update the full screen button UI
     *
     * @param isFullScreen true shows the original icon, otherwise shows the full screen icon
     */
    fun updateFullScreenUI(isFullScreen: Boolean) =
        fullScreenButton.setImageResource(
            if (isFullScreen) {
                R.drawable.ic_original
            } else {
                R.drawable.ic_full_screen
            }
        )

    /**
     * Setup subtitle button
     *
     * @param isShow the addSubtitle icon whether is shown
     * @param isSubtitleShown the subtitle whether is shown
     * @param clickedCallback the callback regarding lock feature
     */
    fun setupSubtitleButton(
        isShow: Boolean,
        isSubtitleShown: Boolean,
        clickedCallback: () -> Unit,
    ) {
        subtitleButton.isVisible = isShow
        updateSubtitleButtonUI(isSubtitleShown)
        subtitleButton.setOnClickListener {
            clickedCallback.invoke()
        }
    }

    /**
     * Set the subtitle button whether is enabled
     *
     * @param enable true is enable, otherwise is false
     */
    fun subtitleButtonEnable(enable: Boolean) {
        subtitleButton.isEnabled = enable
    }

    /**
     * Update the subtitle button icon UI
     *
     * @param isSubtitleShown true is using enabled icon, otherwise is false.
     */
    fun updateSubtitleButtonUI(isSubtitleShown: Boolean) =
        subtitleButton.setImageResource(
            if (isSubtitleShown) {
                R.drawable.ic_subtitles_enable
            } else {
                R.drawable.ic_subtitles_disable
            }
        )

    /**
     * Toggle the playlist button.
     *
     * @param playlistItems the new playlist
     */
    fun togglePlaylistEnabled(playlistItems: List<PlaylistItem>) {
        playlist.visibility =
            if (playlistItems.size > SINGLE_PLAYLIST_SIZE)
                View.VISIBLE
            else
                View.INVISIBLE

    }

    /**
     * Update the visibility of loading animation.
     *
     * @param playbackState the state of player
     */
    fun updateLoadingAnimation(@Player.State playbackState: Int?) {
        container.findViewById<ProgressBar>(R.id.loading).isVisible =
            playbackState == Player.STATE_BUFFERING
    }

    /**
     * Hide player controller.
     */
    @OptIn(UnstableApi::class)
    fun hideController() = playerView.hideController()

    /**
     * Show player controller.
     */
    @OptIn(UnstableApi::class)
    fun showController() = playerView.showController()

    /**
     * Display node metadata.
     *
     * @param metadata metadata to display
     */
    fun displayMetadata(metadata: Metadata) {
        trackName.text = metadata.title ?: metadata.nodeName
    }

    /**
     * Whether is showing track name
     *
     * @param isVisible true is show, otherwise is false
     */
    fun setTrackNameVisible(isVisible: Boolean) {
        trackName.isVisible = isVisible
    }
}
