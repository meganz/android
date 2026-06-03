package mega.privacy.android.feature.videoeditor.presentation.editor.state

import android.net.Uri
import androidx.compose.runtime.Immutable

/** Read-only metadata about the video the user is editing. */
@Immutable
data class SourceState(
    val uri: Uri? = null,
    val durationMs: Long = 0L,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
) {
    val isLoaded: Boolean get() = uri != null && durationMs > 0L

    val aspectRatio: Float
        get() = if (widthPx > 0 && heightPx > 0) widthPx.toFloat() / heightPx.toFloat() else 1f
}
