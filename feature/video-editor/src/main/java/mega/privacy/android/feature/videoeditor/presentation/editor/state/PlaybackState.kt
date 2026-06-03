package mega.privacy.android.feature.videoeditor.presentation.editor.state

import androidx.compose.runtime.Immutable

/** Playback-related state independent of any tool: current play state and playhead. */
@Immutable
data class PlaybackState(
    val isPlaying: Boolean = false,
    val playheadMs: Long = 0L,
)
