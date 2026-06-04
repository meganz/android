package mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim

import androidx.compose.runtime.Immutable

/** In/out points of the current trim selection, in source-time milliseconds. */
@Immutable
data class TrimState(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)

    fun isFullRange(sourceDurationMs: Long): Boolean =
        startMs == 0L && (endMs == sourceDurationMs || endMs == 0L)
}
