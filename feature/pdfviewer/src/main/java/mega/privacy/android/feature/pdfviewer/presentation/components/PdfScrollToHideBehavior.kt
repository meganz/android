package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Chrome visibility for the PDF viewer: visible at the document top, hidden everywhere else.
 * Fed manual scroll samples because the barteksc PDFView does not dispatch Compose nested-scroll.
 */
@Stable
internal class PdfScrollToHideBehavior {

    /**
     * @param atTop true when the document is at its top (derive from `!canScrollVertically(-1)`).
     * @param suppressed true to ignore the sample (e.g. scrubbing or pinch-zooming).
     * @return desired visibility, or `null` when suppressed.
     */
    fun onScroll(atTop: Boolean, suppressed: Boolean): Boolean? =
        if (suppressed) null else atTop
}

@Composable
internal fun rememberPdfScrollToHideBehavior(): PdfScrollToHideBehavior =
    remember { PdfScrollToHideBehavior() }
