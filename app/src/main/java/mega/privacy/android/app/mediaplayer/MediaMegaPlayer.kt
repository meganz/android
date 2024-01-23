package mega.privacy.android.app.mediaplayer

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.utils.CallUtil

/**
 * Player which customized some behaviours in relation to the app needs.
 */
@OptIn(UnstableApi::class)
class MediaMegaPlayer(player: ExoPlayer) : ForwardingPlayer(player) {

    /**
     * Used to identify the first initialization of repeatMode.
     */
    private var initRepeatMode = true

    private val _mediaNotAllowPlayState = MutableStateFlow(false)

    /**
     * Override play(), checks if can proceed with the action in question before calling.
     */
    override fun play() {
        if (canProceed()) {
            super.play()
        }
    }

    /**
     * Override setPlayWhenReady(), when playWhen ready is true, checks if can proceed with the action in question before calling.
     */
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            super.setPlayWhenReady(false)
        } else {
            if (canProceed()) {
                super.setPlayWhenReady(true)
            }
        }
    }

    /**
     * Override seekTo(long positionMs), checks if can proceed with the action in question before calling.
     */
    override fun seekTo(positionInMs: Long) {
        if (canProceed()) {
            super.seekTo(positionInMs)
        }
    }

    /**
     * Override seekTo(int mediaItemIndex, long positionMs), checks if can proceed with the action in question before calling.
     */
    override fun seekTo(windowIndex: Int, positionInMs: Long) {
        if (canProceed()) {
            super.seekTo(windowIndex, positionInMs)
        }
    }

    /**
     * Override seekBack(), checks if can proceed with the action in question before calling.
     */
    override fun seekBack() {
        if (canProceed()) {
            super.seekBack()
        }
    }

    /**
     * Override seekForward(), checks if can proceed with the action in question before calling.
     */
    override fun seekForward() {
        if (canProceed()) {
            super.seekForward()
        }
    }

    /**
     * Override seekToPrevious(), checks if can proceed with the action in question before calling.
     */
    override fun seekToPrevious() {
        if (canProceed()) {
            super.seekToPrevious()
        }
    }

    /**
     * Override seekToNext(), checks if can proceed with the action in question before calling.
     */
    override fun seekToNext() {
        if (canProceed()) {
            super.seekToNext()
        }
    }

    /**
     * ExoPlayer hardcoded switch order to: off -> one -> all,
     * but we need: off -> all -> one, so we need this custom control.
     */
    override fun setRepeatMode(repeatMode: Int) {
        super.setRepeatMode(
            if (initRepeatMode) {
                initRepeatMode = false
                repeatMode
            } else {
                when (this.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                    else -> Player.REPEAT_MODE_OFF
                }
            }
        )
    }

    /**
     * Update the mediaNotAllowPlayState
     *
     * @param value true is not allow play, otherwise is false
     */
    fun updateMediaNotAllowPlayState(value: Boolean) = _mediaNotAllowPlayState.update { value }

    /**
     * Monitor the mediaNotAllowPlayState
     *
     * @return mediaNotAllowPlayState
     */
    fun monitorMediaNotAllowPlayState() = _mediaNotAllowPlayState.asStateFlow()

    /**
     * Checks if can proceed with the action in question.
     * If cannot proceed means it is participating in a call and it will launch
     * an event to show a warning.
     *
     * @return True if it is not participating in a call, false otherwise.
     */
    private fun canProceed(): Boolean =
        CallUtil.participatingInACall().let { mediaNotAllowPlay ->
            _mediaNotAllowPlayState.update { mediaNotAllowPlay }
            !mediaNotAllowPlay
        }
}