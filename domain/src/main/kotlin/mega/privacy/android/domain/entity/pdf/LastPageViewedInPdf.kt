package mega.privacy.android.domain.entity.pdf

/**
 * Represents the last page viewed in a PDF document.
 *
 * @param nodeHandle The unique identifier for the node associated with the PDF.
 * @param lastPageViewed The last page number that was viewed in the PDF.
 */
data class LastPageViewedInPdf(
    val nodeHandle: Long,
    val lastPageViewed: Long,
)
