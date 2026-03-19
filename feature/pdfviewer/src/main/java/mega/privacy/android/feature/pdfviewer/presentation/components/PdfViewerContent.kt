package mega.privacy.android.feature.pdfviewer.presentation.components

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import timber.log.Timber

/**
 * Composable that renders a PDF document using AndroidView wrapping PDFView.
 *
 * @param pdfUri The URI of the PDF to load (for local/streaming)
 * @param pdfBytes The PDF bytes (for in-memory PDFs like encrypted or linked files)
 * @param currentPage The current page to display (1-indexed)
 * @param password The password for protected PDFs
 * @param highlightPageIndex The page index to highlight (0-indexed)
 * @param highlightPdfRects Pre-calculated screen-coordinate RectFs for highlighting the current search match
 * @param onPageChanged Callback when page changes (page: 1-indexed, totalPages)
 * @param onLoadComplete Callback when PDF finishes loading
 * @param onError Callback when an error occurs
 * @param onTap Callback when the PDF is tapped (for toggling toolbar)
 * @param modifier Modifier for the composable
 */
@Composable
fun PdfViewerContent(
    pdfUri: Uri?,
    pdfBytes: ByteArray?,
    currentPage: Int,
    password: String?,
    highlightPageIndex: Int?,
    highlightPdfRects: List<RectF>?,
    onPageChanged: (Int, Int) -> Unit,
    onLoadComplete: (Int) -> Unit,
    onError: (PdfViewerError) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep reference to prevent recreation
    val pdfViewRef: MutableState<PDFView?> = remember { mutableStateOf(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PDFView(context, null).apply {
                // Set LayoutParams to ensure proper measurement
                layoutParams = ViewGroup.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                pdfViewRef.value = this
            }
        },
        update = { pdfView ->
            // If PDF is already loaded, just handle page navigation
            if (pdfView.pageCount > 0) {
                if (pdfView.currentPage != currentPage - 1) {
                    pdfView.jumpTo(currentPage - 1)
                }
                return@AndroidView // Skip full reload
            }

            // Determine source - bytes take precedence (for encrypted/linked files)
            val config = when {
                pdfBytes != null -> pdfView.fromBytes(pdfBytes)
                pdfUri != null -> pdfView.fromUri(pdfUri)
                else -> {
                    Timber.w("No PDF source available")
                    return@AndroidView
                }
            }

            config
                .defaultPage((currentPage - 1).coerceAtLeast(0))
                .onPageChange { page, pageCount ->
                    // page is 0-indexed from library, convert to 1-indexed
                    onPageChanged(page + 1, pageCount)
                }
                .onLoad { pageCount ->
                    Timber.d("PDF loaded with $pageCount pages")
                    onLoadComplete(pageCount)
                }
                .onError { error ->
                    Timber.e(error, "PDF load error")
                    val pdfError = when {
                        error.message?.contains("password", ignoreCase = true) == true ->
                            PdfViewerError.PasswordProtected

                        else -> PdfViewerError.LoadError(error.message ?: "Unknown error")
                    }
                    onError(pdfError)
                }
                .onPageError { page, error ->
                    Timber.e(error, "Cannot load page $page")
                    onError(PdfViewerError.Generic(error))
                }
                .onTap {
                    onTap()
                    true // consume the tap
                }
                .enableAnnotationRendering(true)
                .spacing(10.dp.toPx(pdfView.context).toInt())
                .password(password)
                .load()
        },
        onReset = { pdfView ->
            pdfView.recycle()
        }
    )

    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            pdfViewRef.value?.recycle()
        }
    }
}

/**
 * Extension to convert Dp to pixels
 */
private fun Dp.toPx(context: Context): Float {
    return value * context.resources.displayMetrics.density
}
