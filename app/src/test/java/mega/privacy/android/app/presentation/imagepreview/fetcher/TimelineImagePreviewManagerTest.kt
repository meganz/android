package mega.privacy.android.app.presentation.imagepreview.fetcher

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadFolderHandlesUseCase
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.ListTimelineImageNodeInfoByOffsetUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineImagePreviewManagerTest {

    private val getMediaTimelineSectionsUseCase = mock<GetMediaTimelineSectionsUseCase>()
    private val listTimelineImageNodeInfoByOffsetUseCase =
        mock<ListTimelineImageNodeInfoByOffsetUseCase>()
    private val getCameraUploadFolderHandlesUseCase = mock<GetCameraUploadFolderHandlesUseCase>()
    private val timelineFilterUiStateMapper = TimelineFilterUiStateMapper()

    private lateinit var underTest: TimelineImagePreviewManager

    @BeforeEach
    fun setUp() {
        wheneverBlocking { getCameraUploadFolderHandlesUseCase() }.thenReturn(emptyList())
        underTest = TimelineImagePreviewManager(
            getMediaTimelineSectionsUseCase = getMediaTimelineSectionsUseCase,
            listTimelineImageNodeInfoByOffsetUseCase = listTimelineImageNodeInfoByOffsetUseCase,
            getCameraUploadFolderHandlesUseCase = getCameraUploadFolderHandlesUseCase,
            timelineFilterUiStateMapper = timelineFilterUiStateMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getMediaTimelineSectionsUseCase,
            listTimelineImageNodeInfoByOffsetUseCase,
            getCameraUploadFolderHandlesUseCase,
        )
    }

    @Test
    fun `test that initialize returns the total ref count across sections`() = runTest {
        val sectionA = section("A", 3)
        val sectionB = section("B", 2)
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(sectionA, sectionB))
        whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), eq(sectionA), any(), any(), any()))
            .thenReturn(List(3) { info(100L + it) })
        whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), eq(sectionB), any(), any(), any()))
            .thenReturn(List(2) { info(200L + it) })

        val total = underTest.initialize(
            sort = Sort.NEWEST,
            mediaType = FilterMediaType.ALL_MEDIA,
            source = TimelinePhotosSource.ALL_PHOTOS,
            hideSensitive = false,
        )

        assertThat(total).isEqualTo(5)
    }

    @Test
    fun `test that initialize builds a newest photos filter with show all sensitivity`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        val filterCaptor = argumentCaptor<MediaTimelineFilter>()

        underTest.initialize(
            sort = Sort.NEWEST,
            mediaType = FilterMediaType.IMAGES,
            source = TimelinePhotosSource.ALL_PHOTOS,
            hideSensitive = false,
        )

        verify(getMediaTimelineSectionsUseCase)
            .invoke(filterCaptor.capture(), eq(SortOrder.ORDER_MODIFICATION_DESC))
        assertThat(filterCaptor.firstValue.category).isEqualTo(MediaTimelineFilter.Category.Photos)
        assertThat(filterCaptor.firstValue.sensitivity)
            .isEqualTo(MediaTimelineFilter.Sensitivity.ShowAll)
    }

    @Test
    fun `test that initialize hides sensitive nodes and uses ascending order when requested`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
            val filterCaptor = argumentCaptor<MediaTimelineFilter>()

            underTest.initialize(
                sort = Sort.OLDEST,
                mediaType = FilterMediaType.ALL_MEDIA,
                source = TimelinePhotosSource.ALL_PHOTOS,
                hideSensitive = true,
            )

            verify(getMediaTimelineSectionsUseCase)
                .invoke(filterCaptor.capture(), eq(SortOrder.ORDER_MODIFICATION_ASC))
            assertThat(filterCaptor.firstValue.sensitivity)
                .isEqualTo(MediaTimelineFilter.Sensitivity.HideSensitive)
        }

    @Test
    fun `test that infoAt returns the ref at the global index across sections`() = runTest {
        val sectionA = section("A", 3)
        val sectionB = section("B", 2)
        val a = List(3) { info(100L + it) }
        val b = List(2) { info(200L + it) }
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(sectionA, sectionB))
        whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), eq(sectionA), any(), any(), any()))
            .thenReturn(a)
        whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), eq(sectionB), any(), any(), any()))
            .thenReturn(b)
        underTest.initialize(
            Sort.NEWEST,
            FilterMediaType.ALL_MEDIA,
            TimelinePhotosSource.ALL_PHOTOS,
            false,
        )

        assertThat(underTest.getInfoAtIndex(0)).isEqualTo(a[0])
        assertThat(underTest.getInfoAtIndex(2)).isEqualTo(a[2])
        assertThat(underTest.getInfoAtIndex(3)).isEqualTo(b[0])
        assertThat(underTest.getInfoAtIndex(4)).isEqualTo(b[1])
    }

    @Test
    fun `test that infoAt returns null when the index is out of range`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        underTest.initialize(
            Sort.NEWEST,
            FilterMediaType.ALL_MEDIA,
            TimelinePhotosSource.ALL_PHOTOS,
            false,
        )

        assertThat(underTest.getInfoAtIndex(0)).isNull()
    }

    private fun section(groupId: String, count: Int) = MediaTimelineSection(
        groupId = groupId,
        startDate = 0L,
        endDate = 0L,
        count = count.toLong(),
    )

    private fun info(id: Long) = ImageNodeInfo(id = NodeId(id), name = "node_$id.jpg")
}
