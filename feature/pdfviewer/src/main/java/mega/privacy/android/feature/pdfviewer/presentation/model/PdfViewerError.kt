package mega.privacy.android.feature.pdfviewer.presentation.model

/**
 * Error states for the PDF viewer.
 */
internal sealed class PdfViewerError {
    /**
     * Failed to load PDF file.
     *
     * @param message Internal detail for logging/debugging only. Never displayed to the user.
     */
    data class LoadError(val message: String?) : PdfViewerError()

    /**
     * PDF is password protected
     */
    data object PasswordProtected : PdfViewerError()

    /**
     * Invalid password provided. User can retry indefinitely in the password dialog.
     */
    data object InvalidPassword : PdfViewerError()

    /**
     * File not found
     */
    data object FileNotFound : PdfViewerError()

    /**
     * Network error
     */
    data object NetworkError : PdfViewerError()

    /**
     * Streaming error
     *
     * @param message Internal detail for logging/debugging only. Never displayed to the user.
     */
    data class StreamingError(val message: String?) : PdfViewerError()

    /**
     * Generic error
     *
     * @param throwable Internal detail for logging/debugging only. Never displayed to the user.
     */
    data class Generic(val throwable: Throwable) : PdfViewerError()
}
