package mega.privacy.android.feature.photos.presentation.timeline.revamp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.feature.photos.model.MediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper.MediaTimelineNodeUiItemMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class TimelineRevampViewModelTest {

    private lateinit var underTest: TimelineRevampViewModel

    private val getMediaTimelineSectionsUseCase = mock<GetMediaTimelineSectionsUseCase>()
    private val listMediaNodesByOffsetUseCase = mock<ListMediaNodesByOffsetUseCase>()
    private val mediaTimelineNodeUiItemMapper = mock<MediaTimelineNodeUiItemMapper>()

    private fun initUnderTest() {
        underTest = TimelineRevampViewModel(
            getMediaTimelineSectionsUseCase = getMediaTimelineSectionsUseCase,
            listMediaNodesByOffsetUseCase = listMediaNodesByOffsetUseCase,
            mediaTimelineNodeUiItemMapper = mediaTimelineNodeUiItemMapper,
        )
    }

    @Test
    fun `test that sections are emitted as Data with no loaded nodes initially`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        whenever(getMediaTimelineSectionsUseCase(any())).thenReturn(sections)
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            val data = awaitItem()
            assertThat(data.sections).isEqualTo(sections)
            assertThat(data.loadedNodes).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onVisibleRangeChanged loads and caches the visible nodes`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        val nodes = (0 until 5).map { mock<TypedFileNode>() }
        val items = (0 until 5).map { item(id = it.toLong()) }
        whenever(getMediaTimelineSectionsUseCase(any())).thenReturn(sections)
        whenever(
            listMediaNodesByOffsetUseCase(any(), any(), any(), any(), any())
        ).thenReturn(nodes)
        nodes.forEachIndexed { index, node ->
            whenever(mediaTimelineNodeUiItemMapper(node)).thenReturn(items[index])
        }
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // initial Data with empty loadedNodes
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 4)
            val loaded = awaitItem()
            assertThat(loaded.loadedNodes.keys).containsExactly(0, 1, 2, 3, 4)
            assertThat(loaded.loadedNodes[0]).isEqualTo(items[0])
            cancelAndIgnoreRemainingEvents()
        }

        verify(listMediaNodesByOffsetUseCase).invoke(
            filter = any(),
            section = any(),
            order = eq(SortOrder.ORDER_MODIFICATION_DESC),
            maxElements = eq(5),
            offset = eq(0L),
        )
    }

    @Test
    fun `test that uiState emits Empty when sections are empty`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any())).thenReturn(emptyList())
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Empty>().test {
            assertThat(awaitItem()).isEqualTo(TimelineRevampUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getMediaTimelineSectionsUseCase is invoked with the default filter`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any())).thenReturn(emptyList())
        initUnderTest()

        underTest.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(getMediaTimelineSectionsUseCase).invoke(DEFAULT_FILTER)
    }

    @Test
    fun `test that listMediaNodesByOffsetUseCase is invoked with the default filter`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        val nodes = (0 until 5).map { mock<TypedFileNode>() }
        whenever(getMediaTimelineSectionsUseCase(any())).thenReturn(sections)
        whenever(
            listMediaNodesByOffsetUseCase(any(), any(), any(), any(), any())
        ).thenReturn(nodes)
        nodes.forEach { node ->
            whenever(mediaTimelineNodeUiItemMapper(node)).thenReturn(item(id = 0L))
        }
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem()
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 4)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(listMediaNodesByOffsetUseCase).invoke(
            filter = eq(DEFAULT_FILTER),
            section = any(),
            order = any(),
            maxElements = any(),
            offset = any(),
        )
    }

    private fun section(groupId: String, count: Long) =
        MediaTimelineSection(groupId = groupId, startDate = 0L, endDate = 0L, count = count)

    private fun item(id: Long) = PhotosNodeContentItemV2(
        key = id,
        contentType = PhotosNodeContentType.PhotoNode,
        id = id,
        mediaType = MediaType.Image,
        day = 0,
        month = 0,
        year = 0,
        fullModificationTime = 0L,
        thumbnailFilePath = null,
        previewFilePath = null,
        extension = "jpg",
        isFavourite = false,
        isSensitive = false,
    )

    private companion object {
        val DEFAULT_FILTER = MediaTimelineFilter(
            granularity = MediaTimelineFilter.Granularity.Day,
            category = MediaTimelineFilter.Category.All,
            location = MediaTimelineFilter.Location.CloudDriveAndVault,
            sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
        )
    }
}
