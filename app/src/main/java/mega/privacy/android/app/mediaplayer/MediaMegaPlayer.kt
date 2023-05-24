package mega.privacy.android.app.mediaplayer

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ForwardingPlayer
import com.google.android.exoplayer2.Player
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY

/**
 * Player which customized some behaviours in relation to the app needs.
 */
class MediaMegaPlayer(player: ExoPlayer) : ForwardingPlayer(player) {

    /**
     * Used to identify the first initialization of repeatMode.
     */
    private var initRepeatMode = true

    override fun play() {
        if (canProceed()) {
            super.play()
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            super.setPlayWhenReady(false)
        } else {
            if (canProceed()) {
                super.setPlayWhenReady(true)
            }
        }
    }

    override fun seekTo(positionInMs: Long) {
        if (canProceed()) {
            super.seekTo(positionInMs)
        }
    }

    override fun seekTo(windowIndex: Int, positionInMs: Long) {
        if (canProceed()) {
            super.seekTo(windowIndex, positionInMs)
        }
    }

    override fun seekBack() {
        if (canProceed()) {
            super.seekBack()
        }
    }

    override fun seekForward() {
        if (canProceed()) {
            super.seekForward()
        }
    }

    override fun seekToPrevious() {
        if (canProceed()) {
            super.seekToPrevious()
        }
    }

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
     * Checks if can proceed with the action in question.
     * If cannot proceed means it is participating in a call and it will launch
     * an event to show a warning.
     *
     * @return True if it is not participating in a call, false otherwise.
     */
    private fun canProceed(): Boolean {
        return if (CallUtil.participatingInACall()) {
            LiveEventBus.get(EVENT_NOT_ALLOW_PLAY, Boolean::class.java).post(true)
            false
        } else true
    }
}