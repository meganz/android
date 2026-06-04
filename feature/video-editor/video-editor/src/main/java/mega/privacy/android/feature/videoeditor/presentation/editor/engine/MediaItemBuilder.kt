package mega.privacy.android.feature.videoeditor.presentation.editor.engine

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState

/**
 * Build a [MediaItem] for the player or transformer, applying the Trim slice
 * as a [MediaItem.ClippingConfiguration]. Caller must ensure `state.source.uri`
 * is non-null (we check and throw rather than silently swallowing).
 */
@UnstableApi
fun buildMediaItem(state: EditorState): MediaItem {
    val uri = requireNotNull(state.source.uri) { "Cannot build a MediaItem: source uri is not set" }
    val clipping = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(state.trim.startMs)
        .setEndPositionMs(
            if (state.trim.endMs > 0L) state.trim.endMs else state.source.durationMs,
        )
        .build()
    return MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(clipping)
        .build()
}
