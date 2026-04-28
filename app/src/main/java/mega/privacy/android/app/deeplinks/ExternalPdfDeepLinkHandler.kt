package mega.privacy.android.app.deeplinks

import android.content.Intent
import mega.privacy.android.app.extensions.isHttpScheme
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles external [Intent.ACTION_VIEW] PDF opens, routing to the Compose PDF viewer or the
 * legacy [mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity].
 */
@Singleton
class ExternalPdfDeepLinkHandler @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getFileNameFromStringUriUseCase: GetFileNameFromStringUriUseCase,
) {

    /**
     * If [intent] is an external PDF view, routes to Compose PDF or legacy activity and returns true.
     * Otherwise returns false and leaves [intent] unchanged.
     *
     * Navigation is delegated to the caller via [navigateToComposePdfViewer] so that each Activity
     * instance handles its own back-stack and the shared singleton queue is not involved.
     */
    suspend fun consumeExternalActionViewPdfIfApplicable(
        intent: Intent,
        launchLegacyPdfViewer: () -> Unit,
        navigateToComposePdfViewer: suspend (PdfViewerNavKey) -> Unit,
    ): Boolean {
        if (intent.action != Intent.ACTION_VIEW) return false
        if (!isPdfIntent(intent)) return false
        val uri = intent.data ?: run {
            Timber.w("External PDF: ACTION_VIEW but data is null")
            return false
        }

        val isPdfViewerComposeEnabled = runCatching {
            getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)
        }.getOrDefault(false)

        if (isPdfViewerComposeEnabled) {
            Timber.d("External PDF open: routing to new Compose PDF Viewer")
            val isLocal = !uri.isHttpScheme()
            val rawTitle = getFileNameFromStringUriUseCase(uri.toString()) ?: uri.lastPathSegment
            val resolvedTitle = rawTitle?.let { FileUtil.addPdfFileExtension(it) }
            // Delegate to the caller so each Activity instance navigates within its own task stack.
            navigateToComposePdfViewer(
                PdfViewerNavKey(
                    contentUri = uri.toString(),
                    isLocalContent = isLocal,
                    isExternalFile = true,
                    title = resolvedTitle,
                )
            )
        } else {
            Timber.d("External PDF open: routing to legacy PdfViewerActivity")
            launchLegacyPdfViewer()
        }
        return true
    }

    private fun isPdfIntent(intent: Intent): Boolean {
        val type = intent.type?.lowercase()
        if (type == "application/pdf" || type == "application/x-pdf") return true
        val uri = intent.data ?: return false
        return uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
                || uri.path?.endsWith(".pdf", ignoreCase = true) == true
    }
}
