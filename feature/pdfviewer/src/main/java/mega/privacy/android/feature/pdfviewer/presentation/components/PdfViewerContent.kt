package mega.privacy.android.feature.pdfviewer.presentation.components

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import mega.android.core.ui.theme.supportColor
import mega.android.core.ui.theme.values.SupportColor
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
import timber.log.Timber
import java.io.File

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
internal fun PdfViewerContent(
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

    // Mutable refs so the onDrawAll closure (set once at load time) always reads
    // the latest highlight data without needing to re-register the callback.
    val highlightPageIndexRef = remember { mutableIntStateOf(highlightPageIndex ?: -1) }
    val highlightRectsRef: MutableState<List<RectF>?> =
        remember { mutableStateOf(highlightPdfRects) }
    val highlightColor = supportColor(SupportColor.Warning).copy(alpha = 0.5f)

    // Track highlight identity to trigger redraws without using the generic View.tag
    val lastHighlightIdentity = remember { mutableStateOf<Any?>(null) }

    // Track source identity to detect document changes requiring full reload
    val lastSourceSignature = remember { mutableStateOf<String?>(null) }

    // Highlight paint - yellow fill with 60% opacity for visibility.
    // Keyed on highlightColor so it updates on theme changes.
    val highlightPaint = remember(highlightColor) {
        Paint().apply {
            color = highlightColor.toArgb()
            style = Paint.Style.FILL
        }
    }

    // Padding to add around small highlight rects (in pixels)
    val highlightPadding = 4f

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
            // Always update the mutable refs so the onDrawAll closure sees fresh data
            highlightPageIndexRef.intValue = highlightPageIndex ?: -1
            highlightRectsRef.value = highlightPdfRects

            // Trigger redraw when highlights change (reference equality check)
            if (highlightPdfRects !== lastHighlightIdentity.value) {
                lastHighlightIdentity.value = highlightPdfRects
                pdfView.invalidate()
            }

            // Detect source changes to avoid showing a stale document
            val currentSignature = pdfBytes?.let { "bytes:${it.size}" } ?: pdfUri?.toString()

            // If PDF is already loaded, just handle page navigation or reload on source change
            if (pdfView.pageCount > 0) {
                if (currentSignature == lastSourceSignature.value) {
                    if (pdfView.currentPage != currentPage - 1) {
                        pdfView.jumpTo(currentPage - 1)
                    }
                    return@AndroidView
                }
                // Source changed — recycle and reload below
                Timber.d("PDF source changed, reloading")
            }
            lastSourceSignature.value = currentSignature

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
                .onDrawAll { canvas, pageWidth, pageHeight, displayedPage ->
                    val currentHighlightPage = highlightPageIndexRef.intValue
                    val currentHighlightRects = highlightRectsRef.value
                    if (displayedPage == currentHighlightPage && !currentHighlightRects.isNullOrEmpty()) {
                        currentHighlightRects.forEach { pdfRect ->
                            val canvasRect = pdfView.mapRectToCanvas(
                                displayedPage, pdfRect, pageWidth, pageHeight
                            )
                            val paddedRect = RectF(
                                canvasRect.left - highlightPadding,
                                canvasRect.top - highlightPadding,
                                canvasRect.right + highlightPadding,
                                canvasRect.bottom + highlightPadding
                            )
                            canvas.drawRect(paddedRect, highlightPaint)
                        }
                    }
                }
                .enableAnnotationRendering(true)
                .spacing(10.dp.toPx(pdfView.context).toInt())
                .password(password)
                .load()
        },
        onReset = { pdfView ->
            pdfView.recycle()
            pdfViewRef.value = null
        }
    )

    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            pdfViewRef.value?.recycle()
            pdfViewRef.value = null
        }
    }
}

/**
 * Extension to convert Dp to pixels
 */
private fun Dp.toPx(context: Context): Float {
    return value * context.resources.displayMetrics.density
}

/**
 * Resolves a display [Uri] from [PdfViewerSource] for [PdfViewerContent].
 *
 * In-memory PDF bytes (e.g. after downloading a remote URL) are provided via UI state, not here.
 */
internal fun getPdfUri(source: PdfViewerSource?): Uri? {
    if (source == null) return null

    return when (source) {
        is PdfViewerSource.CloudNode -> {
            val uri = source.contentUri.toUri(source.isLocalContent)
            Timber.d("CloudNode PDF URI: $uri (sourceType=${source.nodeSourceType})")
            uri
        }

        is PdfViewerSource.Offline ->
            Uri.fromFile(File(source.localPath))

        is PdfViewerSource.ChatAttachment -> {
            val uri = source.contentUri.toUri(source.isLocalContent)
            Timber.d("Chat PDF URI: $uri")
            uri
        }

        is PdfViewerSource.FileLink ->
            source.contentUri.toUri(source.isLocalContent)

        is PdfViewerSource.FolderLink -> {
            val uri = source.contentUri.toUri(source.isLocalContent)
            Timber.d("FolderLink PDF URI: $uri")
            uri
        }

        is PdfViewerSource.ZipFile ->
            source.uri

        is PdfViewerSource.ExternalFile ->
            source.uri
    }
}

/**
 * Convert content URI string to Android Uri.
 *
 * @param isLocalContent True if the content is a local file path
 */
private fun String.toUri(isLocalContent: Boolean): Uri {
    return if (isLocalContent) {
        Uri.fromFile(File(this))
    } else {
        Uri.parse(this)
    }
}
