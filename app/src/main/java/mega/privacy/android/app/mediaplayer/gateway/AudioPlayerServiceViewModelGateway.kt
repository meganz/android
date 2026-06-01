package mega.privacy.android.app.mediaplayer.gateway

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * PlayerServiceViewModelGateway for visit AudioPlayerServiceViewModel from outside
 */
interface AudioPlayerServiceViewModelGateway : PlayerServiceViewModelGateway {
    /**
     * Get background play if is enable
     *
     * @return true is enabled, otherwise is false.
     */
    fun backgroundPlayEnabled(): Boolean

    /**
     * Judge the shuffle if is enabled
     *
     * @return true is enabled, otherwise is false.
     */
    fun shuffleEnabled(): Boolean

    /**
     * Get the shuffle order
     *
     * @return ShuffleOrder
     */
    @OptIn(UnstableApi::class)
    fun getShuffleOrder(): ShuffleOrder

    /**
     * Get audio repeat Mode
     *
     * @return RepeatToggleMode
     */
    fun audioRepeatToggleMode(): RepeatToggleMode

    /**
     * Set repeat mode for audio
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun setAudioRepeatMode(repeatToggleMode: RepeatToggleMode)


    /**
     * Set shuffle enable
     *
     * @param enabled true is enabled, otherwise is false
     */
    fun setShuffleEnabled(enabled: Boolean)

    /**
     * Generate the new shuffle order
     *
     * @return new shuffle order
     */
    @OptIn(UnstableApi::class)
    fun newShuffleOrder(): ShuffleOrder

    /**
     * Drop the given item from the Continue Where Left Off index if it is within 2 seconds
     * of [duration]. Covers the case where the user exits or playback ends before the
     * 1-second ticker in TrackAudioPlaybackInfoUseCase gets to delete the entry — including
     * short clips (<=15s) for which the ticker filter never fires.
     *
     * Caveat: in repeat mode ExoPlayer may loop directly without firing STATE_ENDED, or fire
     * it after position has wrapped to 0. In that case the ticker path is the source of truth.
     */
    fun removeRecentlyUsedItemIfNearCompletion(handle: Long, duration: Long, position: Long)
}
