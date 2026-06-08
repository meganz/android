package mega.privacy.android.feature.videoeditor.presentation.editor.export

import android.net.Uri

/** Lifecycle state of an in-flight (or completed) export. */
sealed interface ExportProgress {
    object Idle : ExportProgress
    data class InProgress(val percent: Int) : ExportProgress
    data class Done(val outputUri: Uri) : ExportProgress
    data class Error(val message: String) : ExportProgress
}
