package mega.privacy.android.feature.videoeditor.presentation.editor.engine

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

/**
 * Build the preview's [ExoPlayer] with a [DefaultLoadControl] tuned for the
 * editor's pause/resume pattern.
 *
 * `bufferForPlaybackAfterRebufferMs` is set to 1000ms (down from 5000ms
 * default): the user pauses while editing crops/trims; on long high-bitrate
 * videos the default buffer-recovery delay is the "few seconds before
 * playback resumes" symptom users notice on Apply.
 *
 * Uses [Context.getApplicationContext] internally so the player can't pin the
 * hosting Activity through its internal listeners — callers can pass either
 * an Activity or Application context safely.
 */
@UnstableApi
fun createPreviewPlayer(context: Context): ExoPlayer {
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
            1000,
        )
        .build()
    return ExoPlayer.Builder(context.applicationContext)
        .setLoadControl(loadControl)
        .build()
}
