package mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume

import androidx.compose.runtime.Immutable

/** Audio gain. `1f` = passthrough, `0f` = mute, up to `2f` = +6 dB amplification. */
@Immutable
data class VolumeState(
    val volume: Float = 1f,
) {
    val isIdentity: Boolean get() = volume == 1f
    val isMuted: Boolean get() = volume == 0f
}

/** Preset chips offered in the Volume panel, as percentages. */
val VolumePresetPercents: List<Int> = listOf(0, 50, 100, 150, 200)
