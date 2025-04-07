package mega.privacy.android.app.presentation.transfers.preview.model

import mega.privacy.android.domain.entity.Progress

/**
 * Fake preview state.
 *
 * @param fileName The name of the file.
 * @param fileTypeResId The resource id of the file type icon.
 * @param progress The progress of the transfer.
 * @param previewFilePathToOpen The file path to preview.
 * @param error [Throwable].
 */
data class FakePreviewState(
    val fileName: String? = null,
    val fileTypeResId: Int? = null,
    val progress: Progress = Progress(0f),
    val previewFilePathToOpen: String? = null,
    val error: Throwable? = null,
)
