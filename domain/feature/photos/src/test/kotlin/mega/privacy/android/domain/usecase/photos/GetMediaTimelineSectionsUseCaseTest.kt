package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMediaTimelineSectionsUseCaseTest {

    private lateinit var underTest: GetMediaTimelineSectionsUseCase

    private val photosRepository = mock<PhotosRepository>()

    private val filter = MediaTimelineFilter(
        granularity = Granularity.Month,
        category = Category.All,
        location = Location.CloudDriveAndVault,
        sensitivity = Sensitivity.ShowAll,
    )

    @BeforeEach
    fun setUp() {
        underTest = GetMediaTimelineSectionsUseCase(photosRepository = photosRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository)
    }

    @Test
    fun `test that invoke returns the sections from the repository`() = runTest {
        val sections = listOf(
            MediaTimelineSection(
                groupId = "May 2026",
                startDate = 100L,
                endDate = 200L,
                count = 5L
            ),
            MediaTimelineSection(
                groupId = "April 2026",
                startDate = 10L,
                endDate = 90L,
                count = 3L
            ),
        )
        whenever(photosRepository.getMediaTimelineSections(filter, SortOrder.ORDER_MODIFICATION_DESC))
            .thenReturn(sections)

        assertThat(underTest(filter, SortOrder.ORDER_MODIFICATION_DESC)).isEqualTo(sections)
    }

    @Test
    fun `test that invoke forwards the filter and order to the repository`() = runTest {
        whenever(photosRepository.getMediaTimelineSections(any(), any())).thenReturn(emptyList())

        underTest(filter, SortOrder.ORDER_MODIFICATION_ASC)

        verify(photosRepository).getMediaTimelineSections(filter, SortOrder.ORDER_MODIFICATION_ASC)
    }
}
