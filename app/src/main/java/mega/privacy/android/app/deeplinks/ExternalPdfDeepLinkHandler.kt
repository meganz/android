package mega.privacy.android.app.deeplinks

import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.extensions.isHttpScheme
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles external [Intent.ACTION_VIEW] PDF opens, routing to the Compose PDF viewer.
 */
@Singleton
class ExternalPdfDeepLinkHandler @Inject constructor(
    private val getFileNameFromStringUriUseCase: GetFileNameFromStringUriUseCase,
) {

    /**
     * If [intent] is an external PDF view, routes to Compose PDF viewer and returns true.
     *
     * Navigation is delegated to the caller via [navigateToComposePdfViewer] so that each Activity
     * instance handles its own back-stack and the shared singleton queue is not involved.
     */
    suspend fun consumeExternalActionViewPdfIfApplicable(
        intent: Intent,
        navigateToComposePdfViewer: suspend (PdfViewerNavKey) -> Unit,
    ): Boolean {
        if (intent.action != Intent.ACTION_VIEW) return false
        if (!isPdfIntent(intent)) return false
        val uri = intent.data ?: run {
            Timber.w("External PDF: ACTION_VIEW but data is null")
            return false
        }

        Timber.d("External PDF open: routing to new Compose PDF Viewer")
        // Delegate to the caller so each Activity instance navigates within its own task stack.
        navigateToComposePdfViewer(buildExternalPdfNavKey(uri))
        return true
    }

    /**
     * Build the [PdfViewerNavKey] for an external `ACTION_VIEW` PDF [uri].
     *
     * Public so the same construction logic is reused by replay paths (e.g. after the user
     * authenticates following "Save to Cloud Drive" from a not-logged-in external PDF)
     */
    suspend fun buildExternalPdfNavKey(uri: Uri): PdfViewerNavKey {
        val isLocal = !uri.isHttpScheme()
        val rawTitle = getFileNameFromStringUriUseCase(uri.toString()) ?: uri.lastPathSegment
        val resolvedTitle = rawTitle?.let { FileUtil.addPdfFileExtension(it) }
        return PdfViewerNavKey(
            contentUri = uri.toString(),
            isLocalContent = isLocal,
            isExternalFile = true,
            title = resolvedTitle,
        )
    }

    internal fun isPdfIntent(intent: Intent): Boolean {
        val type = intent.type?.lowercase()
        if (type == "application/pdf" || type == "application/x-pdf") return true
        val uri = intent.data ?: return false
        return uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
                || uri.path?.endsWith(".pdf", ignoreCase = true) == true
    }
}
