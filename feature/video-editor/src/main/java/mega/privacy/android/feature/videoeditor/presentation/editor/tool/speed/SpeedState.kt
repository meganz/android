package mega.privacy.android.feature.videoeditor.presentation.editor.tool.speed

import androidx.compose.runtime.Immutable

/** Playback rate; `1f` is real-time. Slow-mo < 1, fast-forward > 1. */
@Immutable
data class SpeedState(
    val speed: Float = 1f,
) {
    val isIdentity: Boolean get() = speed == 1f
}

/** Preset chips offered in the Speed panel. */
val SpeedOptions: List<Float> = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f)
