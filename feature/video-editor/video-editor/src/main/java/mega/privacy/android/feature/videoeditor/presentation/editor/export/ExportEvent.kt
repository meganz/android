package mega.privacy.android.feature.videoeditor.presentation.editor.export

import android.net.Uri

/**
 * An event emitted by [VideoExporter.export] while encoding. Progress ticks are
 * followed by a single terminal [Completed]; failures are surfaced as the flow
 * throwing rather than as an event. Kept exporter-local so the encoder doesn't
 * depend on any presentation model.
 */
sealed interface ExportEvent {
    data class Progress(val percent: Int) : ExportEvent
    data class Completed(val uri: Uri) : ExportEvent
}
