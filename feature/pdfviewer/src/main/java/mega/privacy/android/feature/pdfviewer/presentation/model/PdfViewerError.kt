package mega.privacy.android.feature.pdfviewer.presentation.model

/**
 * Error states for the PDF viewer.
 */
sealed class PdfViewerError {
    /**
     * Failed to load PDF file
     */
    data class LoadError(val message: String?) : PdfViewerError()

    /**
     * PDF is password protected
     */
    data object PasswordProtected : PdfViewerError()

    /**
     * Invalid password provided
     */
    data class InvalidPassword(val attemptsRemaining: Int) : PdfViewerError()

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
     */
    data class StreamingError(val message: String?) : PdfViewerError()

    /**
     * Generic error
     */
    data class Generic(val throwable: Throwable) : PdfViewerError()
}
