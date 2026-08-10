package mega.privacy.android.feature.pdfviewer.presentation.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PdfPageIndicatorTravelTest {

    @Test
    fun `test that travelScaleForTotalPages returns one when totalPages is one`() {
        assertThat(travelScaleForTotalPages(1)).isEqualTo(1f)
    }

    @Test
    fun `test that travelScaleForTotalPages returns minimum scale when totalPages is two`() {
        assertThat(travelScaleForTotalPages(2)).isEqualTo(PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE)
    }

    @Test
    fun `test that travelScaleForTotalPages ramps linearly between two pages and full travel threshold`() {
        val span = (PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES - 2).toFloat()
        assertThat(travelScaleForTotalPages(3)).isWithin(1e-6f).of(
            PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE +
                    (1f - PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE) * (1f / span)
        )
        assertThat(travelScaleForTotalPages(11)).isWithin(1e-6f).of(
            PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE +
                    (1f - PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE) * (9f / span)
        )
    }

    @Test
    fun `test that travelScaleForTotalPages returns one at full travel threshold and above`() {
        assertThat(travelScaleForTotalPages(PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES)).isEqualTo(1f)
        assertThat(travelScaleForTotalPages(PDF_PAGE_INDICATOR_FULL_TRAVEL_MIN_PAGES + 50)).isEqualTo(
            1f
        )
    }

    @Test
    fun `test that documentToVisualProportion centers band and matches endpoints for damped scale`() {
        val s = PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE
        assertThat(documentToVisualProportion(0f, s)).isWithin(1e-6f).of(0.5f - s / 2f)
        assertThat(documentToVisualProportion(1f, s)).isWithin(1e-6f).of(0.5f + s / 2f)
        assertThat(documentToVisualProportion(0.5f, s)).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun `test that documentToVisualProportion is identity when travel scale is one`() {
        assertThat(documentToVisualProportion(0.12f, 1f)).isWithin(1e-6f).of(0.12f)
        assertThat(documentToVisualProportion(1f, 1f)).isWithin(1e-6f).of(1f)
    }

    @Test
    fun `test that visualToDocumentProportion round trips with documentToVisualProportion`() {
        val scales = floatArrayOf(PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE, 0.5f, 1f)
        val docs = floatArrayOf(0f, 0.17f, 0.5f, 0.91f, 1f)
        for (s in scales) {
            for (u in docs) {
                val v = documentToVisualProportion(u, s)
                val roundTrip = visualToDocumentProportion(v, s)
                assertThat(abs(roundTrip - u)).isLessThan(1e-5f)
            }
        }
    }

    @Test
    fun `test that visualToDocumentProportion returns zero and one at track top and bottom`() {
        assertThat(
            visualToDocumentProportion(
                0f,
                PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE
            )
        ).isEqualTo(0f)
        assertThat(
            visualToDocumentProportion(
                1f,
                PDF_PAGE_INDICATOR_MIN_TRAVEL_SCALE
            )
        ).isEqualTo(1f)
    }

    @Test
    fun `test that mapping stays finite when travel scale is tiny`() {
        val v = documentToVisualProportion(0.75f, 1e-5f)
        val u = visualToDocumentProportion(v, 1e-5f)
        assertThat(u.isFinite()).isTrue()
        assertThat(v.isFinite()).isTrue()
    }
}
