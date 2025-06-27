package mega.privacy.android.data.mapper.pdf

import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LastPageViewedInPdfEntityMapperTest {

    private lateinit var underTest: LastPageViewedInPdfEntityMapper

    @BeforeEach
    internal fun setUp() {
        underTest = LastPageViewedInPdfEntityMapper()
    }

    @Test
    fun `test that maps correctly`() {
        val lastPageViewedInPdf = LastPageViewedInPdf(
            nodeHandle = 12345L,
            lastPageViewed = 10
        )

        val actual = underTest(lastPageViewedInPdf)

        assert(actual.nodeHandle == lastPageViewedInPdf.nodeHandle)
        assert(actual.lastPageViewed == lastPageViewedInPdf.lastPageViewed)
    }
}