package mega.privacy.android.feature.pdfviewer.presentation.components

import android.content.Context
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.notificationsColor
import mega.android.core.ui.theme.supportColor
import mega.android.core.ui.theme.surfaceColor
import mega.android.core.ui.theme.values.NotificationsColor
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
 * @param highlightPdfRects Pre-calculated screen-coordinate RectFs for highlighting the current selected search match
 * @param allMatchRectsByPage PDF-coordinate RectFs for all matches keyed by 0-based page index;
 *        drawn in a lighter colour so every result is visible at a glance
 * @param scrubProgress 0f..1f drag position; null when not actively dragging
 *                      (still null while merely pressed — see [isScrubPressed]).
 * @param isScrubPressed True while the thumb is pressed or dragged; used to stop an in-flight
 *                       fling on press (mirrors legacy DefaultScrollHandle ACTION_DOWN).
 * @param onPageChanged Callback when page changes (page: 1-indexed, totalPages)
 * @param onLoadComplete Callback when PDF finishes loading
 * @param onError Callback when an error occurs
 * @param onTap Callback when the PDF is tapped (for toggling toolbar)
 * @param onChromeVisibilityChange Toggles the chrome based on scroll position (hidden away from
 *        the top); suppressed during scrub/zoom.
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
    allMatchRectsByPage: Map<Int, List<RectF>>,
    scrubProgress: Float?,
    isScrubPressed: Boolean,
    onPageChanged: (Int, Int) -> Unit,
    onLoadComplete: (Int) -> Unit,
    onError: (PdfViewerError) -> Unit,
    onTap: () -> Unit,
    onChromeVisibilityChange: (visible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep reference to prevent recreation
    val pdfViewRef: MutableState<PDFView?> = remember { mutableStateOf(null) }

    // Mutable refs so the onDrawAll closure (set once at load time) always reads
    // the latest highlight data without needing to re-register the callback.
    val highlightPageIndexRef = remember { mutableIntStateOf(highlightPageIndex ?: -1) }
    val highlightRectsRef: MutableState<List<RectF>?> =
        remember { mutableStateOf(highlightPdfRects) }
    val allMatchRectsByPageRef: MutableState<Map<Int, List<RectF>>> =
        remember { mutableStateOf(allMatchRectsByPage) }

    val highLightColor = supportColor(SupportColor.Warning)
    val allMatchColor = notificationsColor(NotificationsColor.NotificationWarning)

    // Track highlight identity to trigger redraws without using the generic View.tag
    val lastHighlightIdentity = remember { mutableStateOf<Any?>(null) }
    val lastAllMatchIdentity = remember { mutableStateOf<Any?>(null) }

    // Track source identity to detect document changes requiring full reload
    val lastSourceSignature = remember { mutableStateOf<String?>(null) }

    // Refs let the load-time onPageScroll closure read the latest callback / scrub state.
    val scrollToHideBehavior = rememberPdfScrollToHideBehavior()
    val onChromeVisibilityRef = remember { mutableStateOf(onChromeVisibilityChange) }
    val scrubbingRef = remember { mutableStateOf(false) }
    // Swallows the library's initial jumpTo(defaultPage) scroll so opening on a restored page
    // doesn't hide the chrome before any user scroll. Re-armed on every (re)load.
    val awaitingInitialPositionRef = remember { mutableStateOf(true) }

    // Paint for the currently-selected match (brighter) and all other matches (lighter).
    // Keyed on their respective colors so they update on theme changes.
    val highlightPaint = remember(highLightColor) {
        Paint().apply {
            color = highLightColor.toArgb()
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
    }
    val allMatchPaint = remember(allMatchColor) {
        Paint().apply {
            color = allMatchColor.toArgb()
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
    }

    // Padding to add around small highlight rects (in pixels)
    val highlightPadding = 4f

    // Divider drawn between pages
    val dividerColorArgb = surfaceColor(SurfaceColor.Surface2).toArgb()

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
            allMatchRectsByPageRef.value = allMatchRectsByPage

            onChromeVisibilityRef.value = onChromeVisibilityChange
            scrubbingRef.value = isScrubPressed || scrubProgress != null

            // Trigger redraw when current-match highlights change
            if (highlightPdfRects !== lastHighlightIdentity.value) {
                lastHighlightIdentity.value = highlightPdfRects
                pdfView.invalidate()
            }

            // Trigger redraw when the all-matches cache gains new pages
            if (allMatchRectsByPage !== lastAllMatchIdentity.value) {
                lastAllMatchIdentity.value = allMatchRectsByPage
                pdfView.invalidate()
            }

            // Detect source changes to avoid showing a stale document
            val currentSignature = pdfBytes?.let { "bytes:${it.size}" } ?: pdfUri?.toString()

            // If PDF is already loaded, just handle page navigation or reload on source change
            if (pdfView.pageCount > 0) {
                if (currentSignature == lastSourceSignature.value) {
                    // Press stops fling without committing a position
                    if (isScrubPressed) {
                        pdfView.stopFling()
                    }
                    if (scrubProgress != null) {
                        // User is dragging the page indicator - scrub continuously.
                        pdfView.setPositionOffset(scrubProgress.coerceIn(0f, 1f), false)
                    } else if (!isScrubPressed && pdfView.currentPage != currentPage - 1) {
                        // Page changed programmatically (e.g. prev/next button). Animate.
                        // !isScrubPressed prevents jumpTo from bouncing back to a stale page when Compose currentPage lags PDFView on press.
                        pdfView.jumpTo(currentPage - 1, true)
                    }
                    return@AndroidView
                }
                // Source changed — recycle and reload below
                Timber.d("PDF source changed, reloading")
            } else if (currentSignature == lastSourceSignature.value) {
                // PDF is still loading asynchronously for this source — avoid a second load() call
                return@AndroidView
            }
            lastSourceSignature.value = currentSignature
            // Re-arm the initial-scroll swallow for the upcoming load.
            awaitingInitialPositionRef.value = true

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
                .onPageScroll { _, _ ->
                    if (awaitingInitialPositionRef.value) {
                        awaitingInitialPositionRef.value = false
                        return@onPageScroll
                    }
                    val atTop = !pdfView.canScrollVertically(-1)
                    val suppressed = scrubbingRef.value || pdfView.isZooming
                    scrollToHideBehavior.onScroll(atTop, suppressed)
                        ?.let { visible -> onChromeVisibilityRef.value(visible) }
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

                    // Draw all match rects, skipping the current match so it is drawn
                    // exactly once below with brighter paint.
                    val allRects = allMatchRectsByPageRef.value[displayedPage]
                    if (!allRects.isNullOrEmpty()) {
                        allRects.forEach { pdfRect ->
                            // RectF.equals() uses float ==, safe here because both lists share
                            // coordinates from the same source with no intermediate calculation.
                            if (displayedPage == currentHighlightPage &&
                                currentHighlightRects?.contains(pdfRect) == true
                            ) return@forEach
                            val canvasRect = pdfView.mapRectToCanvas(
                                displayedPage, pdfRect, pageWidth, pageHeight
                            )
                            canvasRect.left -= highlightPadding
                            canvasRect.top -= highlightPadding
                            canvasRect.right += highlightPadding
                            canvasRect.bottom += highlightPadding
                            canvas.drawRect(canvasRect, allMatchPaint)
                        }
                    }

                    // Draw the currently-selected match on top with brighter paint.
                    if (displayedPage == currentHighlightPage && !currentHighlightRects.isNullOrEmpty()) {
                        currentHighlightRects.forEach { pdfRect ->
                            val canvasRect = pdfView.mapRectToCanvas(
                                displayedPage, pdfRect, pageWidth, pageHeight
                            )
                            canvasRect.left -= highlightPadding
                            canvasRect.top -= highlightPadding
                            canvasRect.right += highlightPadding
                            canvasRect.bottom += highlightPadding
                            canvas.drawRect(canvasRect, highlightPaint)
                        }
                    }
                }
                .enableAnnotationRendering(true)
                .password(password)
                // Compose owns the page indicator; do not attach the legacy Java DefaultScrollHandle.
                .scrollHandle(null)
                .spacing(10.dp.toPx(pdfView.context).toInt())
                .dividerColor(dividerColorArgb)
                .dividerThicknessPx(8.dp.toPx(pdfView.context))
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
            Uri.parse(source.contentUri)

        is PdfViewerSource.ExternalFile ->
            Uri.parse(source.contentUri)
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
