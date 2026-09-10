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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListTimelineImageNodeInfoByOffsetUseCaseTest {

    private lateinit var underTest: ListTimelineImageNodeInfoByOffsetUseCase

    private val photosRepository = mock<PhotosRepository>()

    private val filter = MediaTimelineFilter(
        granularity = Granularity.Month,
        category = Category.All,
        location = Location.CloudDriveAndVault,
        sensitivity = Sensitivity.ShowAll,
    )
    private val section = MediaTimelineSection(
        groupId = "May 2026",
        startDate = 0L,
        endDate = 100L,
        count = 5,
    )

    @BeforeEach
    fun setUp() {
        underTest = ListTimelineImageNodeInfoByOffsetUseCase(
            photosRepository = photosRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository)
    }

    @Test
    fun `test that invoke forwards all parameters to the repository`() = runTest {
        whenever(
            photosRepository.listImageNodeInfoByPage(any(), any(), any(), any(), any())
        ) doReturn emptyList()

        underTest(
            filter = filter,
            section = section,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 60,
            offset = 120L,
        )

        verify(photosRepository).listImageNodeInfoByPage(
            filter = filter,
            section = section,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 60,
            offset = 120L,
        )
    }

    @Test
    fun `test that invoke returns the page unfiltered so it stays index aligned with the grid`() =
        runTest {
            val image = ImageNodeInfo(id = NodeId(1L), name = "a.jpg")
            val svg = ImageNodeInfo(id = NodeId(2L), name = "b.svg")
            val video = ImageNodeInfo(id = NodeId(3L), name = "c.mp4")
            whenever(
                photosRepository.listImageNodeInfoByPage(any(), any(), any(), any(), any())
            ) doReturn listOf(image, svg, video)

            val actual = underTest(
                filter = filter,
                section = section,
                order = SortOrder.ORDER_MODIFICATION_DESC,
                maxElements = 60,
                offset = 0L,
            )

            assertThat(actual).containsExactly(image, svg, video).inOrder()
        }
}
