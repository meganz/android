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
     * Decide whether the given item belongs in the Continue Where Left Off index when the user
     * leaves the player (or playback ends). The item is added only once [position] is past 15
     * seconds and is still more than 3 seconds from [duration]; otherwise it is removed so that
     * briefly opened, finished, or near-completion items are not surfaced back as resumable.
     *
     * Caveat: in repeat mode ExoPlayer may loop directly without firing STATE_ENDED, or fire
     * it after position has wrapped to 0. In that case the ticker path is the source of truth.
     */
    fun saveRecentlyUsedItemIfQualifies(handle: Long, duration: Long, position: Long)
}
