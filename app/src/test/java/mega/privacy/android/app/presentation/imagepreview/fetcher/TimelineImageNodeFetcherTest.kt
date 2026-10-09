package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.imagepreview.fetcher.TimelineImageNodeFetcher.Companion.timelineSortOrderWithDirection
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.ImageNodeInfo
import mega.privacy.android.domain.usecase.FilterCameraUploadImageNodesUseCase
import mega.privacy.android.domain.usecase.FilterCloudDriveImageNodesUseCase
import mega.privacy.android.domain.usecase.photos.ListTimelineImageNodeInfoByOffsetUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineNodesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineImageNodeFetcherTest {

    private val monitorTimelineNodesUseCase = mock<MonitorTimelineNodesUseCase>()
    private val filterCloudDriveImageNodesUseCase = mock<FilterCloudDriveImageNodesUseCase>()
    private val filterCameraUploadImageNodesUseCase = mock<FilterCameraUploadImageNodesUseCase>()
    private val listTimelineImageNodeInfoByOffsetUseCase =
        mock<ListTimelineImageNodeInfoByOffsetUseCase>()
    private val timelineMediaFilterBuilder = mock<TimelineMediaFilterBuilder>()

    private lateinit var underTest: TimelineImageNodeFetcher

    private val oldest = imageNode(id = 1L, modificationTime = 100L)
    private val middle = imageNode(id = 2L, modificationTime = 200L)
    private val newest = imageNode(id = 3L, modificationTime = 300L)

    @BeforeEach
    fun setUp() {
        reset(
            monitorTimelineNodesUseCase,
            filterCloudDriveImageNodesUseCase,
            filterCameraUploadImageNodesUseCase,
            listTimelineImageNodeInfoByOffsetUseCase,
            timelineMediaFilterBuilder,
        )
        whenever(monitorTimelineNodesUseCase()).thenReturn(flowOf(listOf(middle, newest, oldest)))
        underTest = TimelineImageNodeFetcher(
            defaultDispatcher = UnconfinedTestDispatcher(),
            monitorTimelineNodesUseCase = monitorTimelineNodesUseCase,
            filterCloudDriveImageNodesUseCase = filterCloudDriveImageNodesUseCase,
            filterCameraUploadImageNodesUseCase = filterCameraUploadImageNodesUseCase,
            listTimelineImageNodeInfoByOffsetUseCase = listTimelineImageNodeInfoByOffsetUseCase,
            timelineMediaFilterBuilder = timelineMediaFilterBuilder,
        )
    }

    @Test
    fun `test that monitorImageNodes follows the SDK order when the sort order is date added`() =
        runTest {
            stubSdkOrder(SortOrder.ORDER_MODIFICATION_ASC, listOf(3L, 1L, 2L))

            val result = underTest.monitorImageNodes(
                bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_ORDER to "ORDER_MODIFICATION_ASC")
            ).first()

            assertThat(result).containsExactly(newest, oldest, middle).inOrder()
        }

    @Test
    fun `test that monitorImageNodes queries the SDK with the legacy sort type when no sort order is passed`() =
        runTest {
            stubSdkOrder(SortOrder.ORDER_MODIFICATION_ASC, listOf(1L, 2L, 3L))

            val result = underTest.monitorImageNodes(
                bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_TYPE to "OLDEST")
            ).first()

            assertThat(result).containsExactly(oldest, middle, newest).inOrder()
        }

    @Test
    fun `test that monitorImageNodes sorts by modification time when the date added query fails`() =
        runTest {
            whenever(timelineMediaFilterBuilder(any(), any(), any())).thenReturn(mock())
            whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), anyOrNull(), any(), any(), any()))
                .thenThrow(RuntimeException())

            val result = underTest.monitorImageNodes(
                bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_ORDER to "ORDER_MODIFICATION_DESC")
            ).first()

            assertThat(result).containsExactly(newest, middle, oldest).inOrder()
        }

    @Test
    fun `test that monitorImageNodes breaks ascending ties by ascending id when the date added query fails`() =
        runTest {
            val tiedHigherId = imageNode(id = 4L, modificationTime = 200L)
            whenever(monitorTimelineNodesUseCase())
                .thenReturn(flowOf(listOf(middle, newest, tiedHigherId, oldest)))
            whenever(timelineMediaFilterBuilder(any(), any(), any())).thenReturn(mock())
            whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), anyOrNull(), any(), any(), any()))
                .thenThrow(RuntimeException())

            val result = underTest.monitorImageNodes(
                bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_ORDER to "ORDER_MODIFICATION_ASC")
            ).first()

            assertThat(result).containsExactly(oldest, middle, tiedHigherId, newest).inOrder()
        }

    @Test
    fun `test that monitorImageNodes follows the SDK media timestamp order when the sort order is date taken`() =
        runTest {
            val filter = mock<MediaTimelineFilter>()
            whenever(timelineMediaFilterBuilder(any(), any(), eq(false))).thenReturn(filter)
            whenever(
                listTimelineImageNodeInfoByOffsetUseCase(
                    filter = eq(filter),
                    section = anyOrNull(),
                    order = eq(SortOrder.ORDER_MEDIATS_DESC),
                    maxElements = eq(0),
                    offset = eq(0L),
                )
            ).thenReturn(listOf(info(1L), info(3L)))

            val result = underTest.monitorImageNodes(
                bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_ORDER to "ORDER_MEDIATS_DESC")
            ).first()

            assertThat(result).containsExactly(oldest, newest).inOrder()
        }

    @Test
    fun `test that monitorImageNodes keeps the unsorted nodes when the media timestamp query fails`() =
        runTest {
            whenever(timelineMediaFilterBuilder(any(), any(), any())).thenReturn(mock())
            whenever(listTimelineImageNodeInfoByOffsetUseCase(any(), anyOrNull(), any(), any(), any()))
                .thenThrow(RuntimeException())

            val result = underTest.monitorImageNodes(
                bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_ORDER to "ORDER_MEDIATS_ASC")
            ).first()

            assertThat(result).containsExactly(middle, newest, oldest).inOrder()
        }

    @ParameterizedTest(name = "{0} ascending={1} -> {2}")
    @MethodSource("sortOrderWithDirectionCases")
    fun `test that timelineSortOrderWithDirection keeps the timestamp column and applies the direction`(
        gridSortOrder: String?,
        ascending: Boolean,
        expected: SortOrder,
    ) {
        val bundle = gridSortOrder?.let {
            bundleOf(TimelineImageNodeFetcher.TIMELINE_SORT_ORDER to it)
        } ?: bundleOf()

        assertThat(bundle.timelineSortOrderWithDirection(ascending)).isEqualTo(expected)
    }

    private fun sortOrderWithDirectionCases() = Stream.of(
        Arguments.of("ORDER_MEDIATS_DESC", true, SortOrder.ORDER_MEDIATS_ASC),
        Arguments.of("ORDER_MEDIATS_ASC", false, SortOrder.ORDER_MEDIATS_DESC),
        Arguments.of("ORDER_MODIFICATION_DESC", true, SortOrder.ORDER_MODIFICATION_ASC),
        Arguments.of("ORDER_MODIFICATION_ASC", false, SortOrder.ORDER_MODIFICATION_DESC),
        Arguments.of(null, false, SortOrder.ORDER_MODIFICATION_DESC),
    )

    private suspend fun stubSdkOrder(order: SortOrder, ids: List<Long>) {
        val filter = mock<MediaTimelineFilter>()
        whenever(timelineMediaFilterBuilder(any(), any(), eq(false))).thenReturn(filter)
        whenever(
            listTimelineImageNodeInfoByOffsetUseCase(
                filter = eq(filter),
                section = anyOrNull(),
                order = eq(order),
                maxElements = eq(0),
                offset = eq(0L),
            )
        ).thenReturn(ids.map(::info))
    }

    private fun bundleOf(vararg params: Pair<String, String>) = mock<Bundle> {
        params.forEach { (key, value) -> on { getString(key) } doReturn value }
    }

    private fun imageNode(id: Long, modificationTime: Long) = mock<ImageNode> {
        on { this.id } doReturn NodeId(id)
        on { this.modificationTime } doReturn modificationTime
    }

    private fun info(id: Long) = ImageNodeInfo(id = NodeId(id), name = "$id")
}
