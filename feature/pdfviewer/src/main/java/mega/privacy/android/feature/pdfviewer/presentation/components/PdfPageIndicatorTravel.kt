package mega.privacy.android.feature.pdfviewer.presentation.components

/*
 * PDF page indicator — travel dampening (thumb vertical range vs document scroll).
 *
 * Travel scale s is the fraction of the track (0 = top, 1 = bottom of the draggable range)
 * between the thumb for the first page (document proportion u = 0) and the last page (u = 1).
 * Equivalently, s is how much of the full track height is used for that full sweep: with
 * v = 0.5 + (u - 0.5) * s we get v(u=1) - v(u=0) = s.
 *
 * With PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE = 0.28 and PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES = 12,
 * travelScaleForTotalPages ramps s linearly in (pages - 2):
 *
 *   s = 28% + (100% - 28%) * (pages - 2) / 10
 *
 * …for 2 ≤ pages ≤ 11, and s = 100% for 12+ pages (same as undamped full-height travel).
 *
 * Reference — s as % of track used for the full document sweep (page 1 → last):
 *   2→28%, 3→35.2%, 4→42.4%, 5→49.6%, 6→56.8%, 7→64%, 8→71.2%, 9→78.4%, 10→85.6%, 11→92.8%, 12+→100%.
 *
 * Example - 2 pages (s = 28%): first page u = 0 -> v = 50% - 14% = 36% from top; last page u = 1 ->
 * v = 50% + 14% = 64% from top. The thumb stays in a 28%-tall band centered at 50% (36% to 64%).
 *
 * Example - 12+ pages (s = 100%): thumb can use the full track from 0% to 100%.
 */

/** Below this page count, indicator travel interpolates toward full track height. */
internal const val PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES = 12

/** Tightest vertical band (e.g. 2-page documents); ramps up toward 1 by [PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES]. */
internal const val PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE = 0.28f

private const val TRAVEL_SCALE_EPSILON = 1e-4f

/**
 * Maps [totalPages] to travel scale `s`: 1f means full top-to-bottom thumb travel;
 * smaller values compress movement around the vertical center.
 */
internal fun travelScaleForTotalPages(totalPages: Int): Float {
    if (totalPages <= 1) return 1f
    if (totalPages >= PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES) return 1f
    val span = (PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES - 2).coerceAtLeast(1)
    val t = ((totalPages - 2).toFloat() / span).coerceIn(0f, 1f)
    return PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE +
            (1f - PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE) * t
}

internal fun documentToVisualProportion(document: Float, travelScale: Float): Float {
    val s = travelScale.coerceAtLeast(TRAVEL_SCALE_EPSILON)
    return 0.5f + (document - 0.5f) * s
}

internal fun visualToDocumentProportion(visual: Float, travelScale: Float): Float {
    val s = travelScale.coerceAtLeast(TRAVEL_SCALE_EPSILON)
    return ((visual - 0.5f) / s + 0.5f).coerceIn(0f, 1f)
}
