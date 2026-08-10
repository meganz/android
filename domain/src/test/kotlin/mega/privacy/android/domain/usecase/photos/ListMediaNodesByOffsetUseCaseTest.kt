package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Category
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.TypedFileNode
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
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListMediaNodesByOffsetUseCaseTest {

    private lateinit var underTest: ListMediaNodesByOffsetUseCase

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
    private val offset = 120L

    @BeforeEach
    fun setUp() {
        underTest = ListMediaNodesByOffsetUseCase(photosRepository = photosRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository)
    }

    @Test
    fun `test that invoke forwards all parameters to the repository`() = runTest {
        whenever(
            photosRepository.listMediaNodesByPage(any(), any(), any(), any(), any())
        ) doReturn emptyList()

        underTest(
            filter = filter,
            section = section,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 60,
            offset = offset,
        )

        verify(photosRepository).listMediaNodesByPage(
            filter = filter,
            section = section,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 60,
            offset = offset,
        )
    }

    @Test
    fun `test that invoke keeps images and videos but filters out svg`() = runTest {
        val image = nodeOf(StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"))
        val svg = nodeOf(SvgFileTypeInfo(mimeType = "image/svg+xml", extension = "svg"))
        val video = nodeOf(
            VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = Duration.ZERO)
        )
        whenever(
            photosRepository.listMediaNodesByPage(any(), any(), any(), any(), any())
        ) doReturn listOf(image, svg, video)

        val actual = underTest(
            filter = filter,
            section = section,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 60,
            offset = 0L,
        )

        assertThat(actual).containsExactly(image, video).inOrder()
    }

    private fun nodeOf(fileType: FileTypeInfo) = mock<TypedFileNode> {
        on { type } doReturn fileType
    }
}
