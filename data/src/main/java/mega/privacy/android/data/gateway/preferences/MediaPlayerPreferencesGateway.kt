package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow


/**
 * Media player preferences gate way
 */
interface MediaPlayerPreferencesGateway {
    /**
     * Monitor the value of AudioBackgroundPlayEnabled
     *
     * @return Flow of Boolean
     */
    fun monitorAudioBackgroundPlayEnabled(): Flow<Boolean?>

    /**
     * Set the value of AudioBackgroundPlayEnabled
     *
     * @param value true is enable audio background play, otherwise is false.
     */
    suspend fun setAudioBackgroundPlayEnabled(value: Boolean)

    /**
     * Monitor the value of AudioShuffleEnabled
     *
     * @return Flow of Boolean
     */
    fun monitorAudioShuffleEnabled(): Flow<Boolean?>

    /**
     * Set the value of AudioShuffleEnabled
     *
     * @param value true is shuffled, otherwise is false.
     */
    suspend fun setAudioShuffleEnabled(value: Boolean)

    /**
     * Monitor the value of AudioRepeatMode
     *
     * @return Flow of Int
     */
    fun monitorAudioRepeatMode(): Flow<Int?>

    /**
     * Set the value of AudioRepeatMode
     *
     * @param value Int value of audio repeat mode
     */
    suspend fun setAudioRepeatMode(value: Int)

    /**
     * Monitor the value of VideoRepeatMode
     *
     * @return Flow of Int
     */
    fun monitorVideoRepeatMode(): Flow<Int?>

    /**
     * Set the value of VideoRepeatMode
     *
     * @param value Int value of video repeat mode
     */
    suspend fun setVideoRepeatMode(value: Int)
}