package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import nz.mega.sdk.MegaGroupNodesByDateFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MediaTimelineFilterMapperTest {

    private val mediaTimelineGranularityIntMapper = mock<MediaTimelineGranularityIntMapper>()
    private val mediaTimelineCategoryIntMapper = mock<MediaTimelineCategoryIntMapper>()
    private val mediaTimelineSensitivityIntMapper = mock<MediaTimelineSensitivityIntMapper>()
    private val mediaTimelineLocationIntMapper = mock<MediaTimelineLocationIntMapper>()

    private val underTest = MediaTimelineFilterMapper(
        mediaTimelineGranularityIntMapper = mediaTimelineGranularityIntMapper,
        mediaTimelineCategoryIntMapper = mediaTimelineCategoryIntMapper,
        mediaTimelineSensitivityIntMapper = mediaTimelineSensitivityIntMapper,
        mediaTimelineLocationIntMapper = mediaTimelineLocationIntMapper,
    )

    @Test
    fun `test that granularity category sensitivity and location are applied to the sdk filter`() {
        val filter = MediaTimelineFilter(
            granularity = Granularity.Month,
            category = Category.All,
            location = Location.CloudDriveAndVault,
            sensitivity = Sensitivity.ShowAll,
        )
        whenever(mediaTimelineGranularityIntMapper(Granularity.Month)).thenReturn(GRANULARITY)
        whenever(mediaTimelineCategoryIntMapper(Category.All)).thenReturn(CATEGORY)
        whenever(mediaTimelineSensitivityIntMapper(Sensitivity.ShowAll)).thenReturn(SENSITIVITY)
        whenever(mediaTimelineLocationIntMapper(Location.CloudDriveAndVault)).thenReturn(LOCATION)

        val sdkFilter = mock<MegaGroupNodesByDateFilter>()
        mockStatic(MegaGroupNodesByDateFilter::class.java).use { mockedStatic ->
            mockedStatic.`when`<MegaGroupNodesByDateFilter> {
                MegaGroupNodesByDateFilter.createInstance()
            }.thenReturn(sdkFilter)

            underTest(filter)

            verify(sdkFilter).byGranularity(GRANULARITY)
            verify(sdkFilter).byCategory(CATEGORY)
            verify(sdkFilter).bySensitivity(SENSITIVITY)
            verify(sdkFilter).byLocation(LOCATION)
        }
    }

    private companion object {
        private const val GRANULARITY = 2
        private const val CATEGORY = 13
        private const val SENSITIVITY = 0
        private const val LOCATION = 1
    }
}
