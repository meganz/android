package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Voice clip player class.
 * It contains a map of [MediaPlayer] to play different voice clips.
 * It also contains functions to play, pause, seekTo, etc.
 */
class VoiceClipPlayer @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Map of [MediaPlayer] to play different voice clips.
     * The key is the message ID of voice clip message.
     */
    private val mediaPlayerMap = mutableMapOf<Long, MediaPlayer>()

    private fun getMediaPlayer(key: Long): MediaPlayer =
        mediaPlayerMap.getOrPut(key) { MediaPlayer() }

    /**
     * Pause the voice clip with the given message ID.
     *
     * @param key The message ID of voice clip message.
     */
    fun pause(key: Long) {
        getMediaPlayer(key).pause()
    }

    /**
     * Get the play progress of current voice clip
     *
     * @param key
     * @return position in milliseconds
     */
    fun getCurrentPosition(key: Long): Int = getMediaPlayer(key).currentPosition

    /**
     * Check if it's playing or not
     *
     * @param key
     * @return true if it is playing. Otherwise, false.
     */
    fun isPlaying(key: Long): Boolean = getMediaPlayer(key).isPlaying

    /**
     * Seek to the given position
     *
     * @param key
     * @param position position in milliseconds
     */
    fun seekTo(key: Long, position: Int) =
        getMediaPlayer(key).seekTo(position)


    /**
     * Play the voice clip with the given message ID.
     *
     * @param key
     * @param path absolute file path of the voice clip
     * @param pos set this position to start playing. Default is 0 meaning start from beginning.
     * @return a flow of [VoiceClipPlayState] to monitor the play state and progress
     */
    fun play(key: Long, path: String, pos: Int = 0): Flow<VoiceClipPlayState> =
        callbackFlow {
            val onPreparedListener = MediaPlayer.OnPreparedListener {
                trySend(VoiceClipPlayState.Prepared)
                getMediaPlayer(key).seekTo(pos)
                getMediaPlayer(key).start()
                launch { startPlayingUpdates(key, this@callbackFlow) }
            }
            val onCompletionListener = MediaPlayer.OnCompletionListener {
                trySend(VoiceClipPlayState.Completed)
                close()
            }
            val onErrorListener = MediaPlayer.OnErrorListener { _, what, extra ->
                trySend(VoiceClipPlayState.Error(Exception("what: $what, extra: $extra")))
                cancel()
                true
            }
            getMediaPlayer(key).setOnCompletionListener(onCompletionListener)
            getMediaPlayer(key).setOnErrorListener(onErrorListener)
            getMediaPlayer(key).setOnPreparedListener(onPreparedListener)

            getMediaPlayer(key).reset()
            getMediaPlayer(key).setDataSource(path)
            trySend(VoiceClipPlayState.Idle)
            getMediaPlayer(key).prepare()

            awaitClose {
                removeCallbacks(key)
                getMediaPlayer(key).reset()
            }
        }.flowOn(ioDispatcher)

    private fun removeCallbacks(key: Long) {
        getMediaPlayer(key).setOnCompletionListener(null)
        getMediaPlayer(key).setOnErrorListener(null)
        getMediaPlayer(key).setOnPreparedListener(null)
    }

    /**
     * Release all media player resources
     */
    fun releaseAll() {
        mediaPlayerMap.values.forEach { it.release() }
        mediaPlayerMap.clear()
    }

    /**
     * Once has started playing a voice clip, this function start a loop to poll the current position.
     *
     * @param key
     * @param scope the scope used to emit the play state into the flow.
     */
    private suspend fun startPlayingUpdates(
        key: Long,
        scope: ProducerScope<VoiceClipPlayState>,
    ) {
        while (getMediaPlayer(key).isPlaying) {
            scope.trySend(
                VoiceClipPlayState.Playing(
                    getMediaPlayer(key).currentPosition
                )
            )
            delay(50L)
        }
    }
}