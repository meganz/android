package mega.privacy.android.app.presentation.imagepreview.fetcher

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.usecase.GetImageNodeByIdUseCase
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineImagePreviewManagerTest {

    private val getMediaTimelineSectionsUseCase = mock<GetMediaTimelineSectionsUseCase>()
    private val listTimelineImageNodeInfoByOffsetUseCase =
        mock<ListTimelineImageNodeInfoByOffsetUseCase>()
    private val getImageNodeByIdUseCase = mock<GetImageNodeByIdUseCase>()
    private val getCameraUploadFolderHandlesUseCase = mock<GetCameraUploadFolderHandlesUseCase>()
    private val timelineFilterUiStateMapper = TimelineFilterUiStateMapper()

    private lateinit var underTest: TimelineImagePreviewManager

    @BeforeEach
    fun setUp() {
        wheneverBlocking { getCameraUploadFolderHandlesUseCase() }.thenReturn(emptyList())
        underTest = TimelineImagePreviewManager(
            getMediaTimelineSectionsUseCase = getMediaTimelineSectionsUseCase,
            listTimelineImageNodeInfoByOffsetUseCase = listTimelineImageNodeInfoByOffsetUseCase,
            getImageNodeByIdUseCase = getImageNodeByIdUseCase,
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
            getImageNodeByIdUseCase,
            getCameraUploadFolderHandlesUseCase,
        )
    }

    @Test
    fun `test that initialize returns the total media count across sections`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section("A", 3), section("B", 2)))

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
    fun `test that getImageNodeAtIndex pages the ref list and resolves the node by id`() = runTest {
        val node = mock<ImageNode>()
        stubSections(total = 100)
        stubRefPages()
        whenever(getImageNodeByIdUseCase(NodeId(5L))).thenReturn(node)
        underTest.initialize(Sort.NEWEST, FilterMediaType.ALL_MEDIA, TimelinePhotosSource.ALL_PHOTOS, false)

        val result = underTest.getImageNodeAtIndex(5)

        assertThat(result).isEqualTo(node)
        verify(getImageNodeByIdUseCase).invoke(NodeId(5L))
    }

    @Test
    fun `test that getImageNodeAtIndex returns null without paging when the index is out of range`() =
        runTest {
            stubSections(total = 10)
            stubRefPages()
            underTest.initialize(Sort.NEWEST, FilterMediaType.ALL_MEDIA, TimelinePhotosSource.ALL_PHOTOS, false)

            val result = underTest.getImageNodeAtIndex(20)

            assertThat(result).isNull()
            verifyNoInteractions(listTimelineImageNodeInfoByOffsetUseCase)
        }

    @Test
    fun `test that indexOfImageNode returns the index where the node actually sits`() = runTest {
        stubSections(total = 100)
        stubRefPages()
        underTest.initialize(Sort.NEWEST, FilterMediaType.ALL_MEDIA, TimelinePhotosSource.ALL_PHOTOS, false)

        val index = underTest.indexOfImageNode(preferredIndex = 5, nodeId = NodeId(7L))

        assertThat(index).isEqualTo(7)
    }

    @Test
    fun `test that indexOfImageNode falls back to the preferred index when the node is not loaded`() =
        runTest {
            stubSections(total = 100)
            stubRefPages()
            underTest.initialize(Sort.NEWEST, FilterMediaType.ALL_MEDIA, TimelinePhotosSource.ALL_PHOTOS, false)

            val index = underTest.indexOfImageNode(preferredIndex = 5, nodeId = NodeId(999L))

            assertThat(index).isEqualTo(5)
        }

    @Test
    fun `test that getImageNode resolves the node by id`() = runTest {
        val node = mock<ImageNode>()
        whenever(getImageNodeByIdUseCase(NodeId(7L))).thenReturn(node)

        val result = underTest.getImageNode(NodeId(7L))

        assertThat(result).isEqualTo(node)
        verify(getImageNodeByIdUseCase).invoke(NodeId(7L))
    }

    private suspend fun stubSections(total: Int) {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section("A", total)))
    }

    private suspend fun stubRefPages() {
        whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), eq(null), any(), any(), any()))
            .thenAnswer { invocation ->
                val maxElements = invocation.getArgument<Int>(3)
                val offset = invocation.getArgument<Long>(4)
                (offset until offset + maxElements).map { ImageNodeInfo(NodeId(it), "n$it.jpg") }
            }
    }

    private fun section(groupId: String, count: Int) = MediaTimelineSection(
        groupId = groupId,
        startDate = 0L,
        endDate = 0L,
        count = count.toLong(),
    )
}
