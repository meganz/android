package mega.privacy.android.data.mapper.pdf

import mega.privacy.android.data.database.entity.LastPageViewedInPdfEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LastPageViewedInPdfModelMapperTest {

    private lateinit var underTest: LastPageViewedInPdfModelMapper

    @BeforeEach
    internal fun setUp() {
        underTest = LastPageViewedInPdfModelMapper()
    }

    @Test
    fun `test that maps correctly`() {
        val lastPageViewedInPdfEntity = LastPageViewedInPdfEntity(
            nodeHandle = 12345L,
            lastPageViewed = 10
        )

        val actual = underTest(lastPageViewedInPdfEntity)

        assert(actual.nodeHandle == lastPageViewedInPdfEntity.nodeHandle)
        assert(actual.lastPageViewed == lastPageViewedInPdfEntity.lastPageViewed)
    }
}