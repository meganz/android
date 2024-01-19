package mega.privacy.android.app.mediaplayer.gateway

import androidx.media3.exoplayer.source.ShuffleOrder
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * PlayerServiceViewModelGateway for visit AudioPlayerServiceViewModel from outside
 */
interface AudioPlayerServiceViewModelGateway : PlayerServiceViewModelGateway {

    /**
     * Toggle backgroundPlayEnabled
     *
     * @param isEnable true is enable, otherwise is disable
     * @return backgroundPlayEnabled after toggled
     */
    fun toggleBackgroundPlay(isEnable: Boolean): Boolean

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
    fun newShuffleOrder(): ShuffleOrder
}