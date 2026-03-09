package mega.privacy.android.feature.photos.presentation.videos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.ClearRecentlyWatchedVideosUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoRecentlyWatchedUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
class VideoRecentlyWatchedViewModelTest {

    private lateinit var underTest: VideoRecentlyWatchedViewModel

    private val monitorVideoRecentlyWatchedUseCase = mock<MonitorVideoRecentlyWatchedUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val videoUiEntityMapper = mock<VideoUiEntityMapper>()
    private val clearRecentlyWatchedVideosUseCase = mock<ClearRecentlyWatchedVideosUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val getNodeContentUriByHandleUseCase = mock<GetNodeContentUriByHandleUseCase>()
    private val nodeSourceTypeToViewTypeMapper = NodeSourceTypeToViewTypeMapper()

    @BeforeEach
    fun setUp() {
        underTest = VideoRecentlyWatchedViewModel(
            monitorVideoRecentlyWatchedUseCase = monitorVideoRecentlyWatchedUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            videoUiEntityMapper = videoUiEntityMapper,
            clearRecentlyWatchedVideosUseCase = clearRecentlyWatchedVideosUseCase,
            getNodeContentUriByHandleUseCase = getNodeContentUriByHandleUseCase,
            nodeSourceTypeToViewTypeMapper = nodeSourceTypeToViewTypeMapper,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorVideoRecentlyWatchedUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase,
            videoUiEntityMapper,
            clearRecentlyWatchedVideosUseCase,
            monitorOfflineNodeUpdatesUseCase,
            getNodeContentUriByHandleUseCase,
        )
    }

    @Test
    fun `test that the initial state is loading`() = runTest {
        monitorNodeUpdatesUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorOfflineNodeUpdatesUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorHiddenNodesEnabledUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorShowHiddenItemsUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorVideoRecentlyWatchedUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideoRecentlyWatchedUiState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState emits Loading then Data state with grouped videos`() = runTest {
        val videoNode1 = mock<TypedVideoNode> {
            on { id }.thenReturn(NodeId(1L))
            on { watchedTimestamp }.thenReturn(TimeUnit.DAYS.toSeconds(1))
        }
        val videoNode2 = mock<TypedVideoNode> {
            on { id }.thenReturn(NodeId(2L))
            on { watchedTimestamp }.thenReturn(TimeUnit.DAYS.toSeconds(1))
        }
        val videoEntity1 = mock<VideoUiEntity> {
            on { id }.thenReturn(NodeId(1L))
            on { watchedDate }.thenReturn(TimeUnit.DAYS.toSeconds(1))
        }
        val videoEntity2 = mock<VideoUiEntity> {
            on { id }.thenReturn(NodeId(2L))
            on { watchedDate }.thenReturn(TimeUnit.DAYS.toSeconds(1))
        }

        stubInitialValues()
        whenever(videoUiEntityMapper(videoNode1, emptyList())).thenReturn(videoEntity1)
        whenever(videoUiEntityMapper(videoNode2, emptyList())).thenReturn(videoEntity2)
        whenever(monitorVideoRecentlyWatchedUseCase()).thenReturn(
            flowOf(listOf(videoNode1, videoNode2))
        )

        underTest.uiState.test {
            val dataState = awaitItem() as? VideoRecentlyWatchedUiState.Data
            if (dataState != null) {
                assertThat(dataState.groupedVideoRecentlyWatchedItems).hasSize(1)
                assertThat(dataState.groupedVideoRecentlyWatchedItems[1L]).containsExactly(
                    videoEntity1,
                    videoEntity2
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `test that uiState Data reflects showHiddenItems from combine flow`() = runTest {
        stubInitialValues()

        underTest.uiState.test {
            val dataState = awaitItem() as? VideoRecentlyWatchedUiState.Data
            if (dataState != null) {
                assertThat(dataState.showHiddenItems).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `test that clearVideosRecentlyWatched invokes use case`() = runTest {
        whenever(clearRecentlyWatchedVideosUseCase()).thenReturn(Unit)

        underTest.clearVideosRecentlyWatched()
        advanceUntilIdle()

        verify(clearRecentlyWatchedVideosUseCase).invoke()
    }

    @Test
    fun `test that clearRecentlyWatchedEvent is triggered when clearVideosRecentlyWatched succeeds`() =
        runTest {
            whenever(clearRecentlyWatchedVideosUseCase()).thenReturn(Unit)

            underTest.clearRecentlyWatchedEvent.test {
                assertThat(awaitItem()).isEqualTo(consumed)
                underTest.clearVideosRecentlyWatched()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that clearRecentlyWatchedEvent stays consumed when clearVideosRecentlyWatched fails`() =
        runTest {
            whenever(clearRecentlyWatchedVideosUseCase()).thenThrow(RuntimeException("error"))

            underTest.clearVideosRecentlyWatched()
            advanceUntilIdle()

            assertThat(underTest.clearRecentlyWatchedEvent.value).isEqualTo(consumed)
        }

    @Test
    fun `test that resetVideosRecentlyWatched sets clearRecentlyWatchedEvent to consumed`() =
        runTest {
            whenever(clearRecentlyWatchedVideosUseCase()).thenReturn(Unit)

            underTest.clearRecentlyWatchedEvent.test {
                assertThat(awaitItem()).isEqualTo(consumed)
                underTest.clearVideosRecentlyWatched()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(triggered)
                underTest.resetVideosRecentlyWatched()
                assertThat(awaitItem()).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that navigateToVideoPlayerEvent initial state is consumed`() = runTest {
        assertThat(underTest.navigateToVideoPlayerEvent.value).isEqualTo(consumed())
    }

    @Test
    fun `test that onItemClicked triggers navigateToVideoPlayerEvent when getNodeContentUriByHandleUseCase returns uri`() =
        runTest {
            val videoItem = createVideoUiEntity(handle = 1L, name = "test_video")
            val typedNode = mock<TypedVideoNode> { on { id }.thenReturn(NodeId(1L)) }
            val contentUri = NodeContentUri.RemoteContentUri("http://test.url", false)
            stubInitialValues()
            whenever(monitorVideoRecentlyWatchedUseCase()).thenReturn(
                flowOf(listOf(typedNode))
            )
            whenever(videoUiEntityMapper(any(), any())).thenReturn(videoItem)
            whenever(getNodeContentUriByHandleUseCase(1L)).thenReturn(contentUri)

            underTest.uiState
                .filterIsInstance<VideoRecentlyWatchedUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemClicked(videoItem)
                    advanceUntilIdle()

                    val event = underTest.navigateToVideoPlayerEvent.value
                    assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                    val (item, uri) = (event as StateEventWithContentTriggered).content
                    assertThat(item).isEqualTo(videoItem)
                    assertThat(uri).isEqualTo(contentUri)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that onItemClicked does not trigger navigateToVideoPlayerEvent when getNodeContentUriByHandleUseCase throws`() =
        runTest {
            val videoItem = createVideoUiEntity(handle = 1L)
            stubInitialValues()
            whenever(getNodeContentUriByHandleUseCase(any())).thenThrow(RuntimeException("test"))

            underTest.uiState
                .filterIsInstance<VideoRecentlyWatchedUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemClicked(videoItem)
                    advanceUntilIdle()

                    assertThat(underTest.navigateToVideoPlayerEvent.value).isEqualTo(consumed())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that resetNavigateToVideoPlayer sets navigateToVideoPlayerEvent to consumed`() =
        runTest {
            val videoItem = createVideoUiEntity(handle = 1L)
            val typedNode = mock<TypedVideoNode> {
                on { id }.thenReturn(NodeId(1L))
            }
            val contentUri = NodeContentUri.RemoteContentUri("http://test.url", false)
            stubInitialValues()
            whenever(monitorVideoRecentlyWatchedUseCase()).thenReturn(
                flowOf(listOf(typedNode))
            )
            whenever(videoUiEntityMapper(any(), any())).thenReturn(videoItem)
            whenever(getNodeContentUriByHandleUseCase(1L)).thenReturn(contentUri)

            underTest.uiState
                .filterIsInstance<VideoRecentlyWatchedUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemClicked(videoItem)
                    advanceUntilIdle()
                    assertThat(underTest.navigateToVideoPlayerEvent.value)
                        .isInstanceOf(StateEventWithContentTriggered::class.java)

                    underTest.resetNavigateToVideoPlayer()
                    assertThat(underTest.navigateToVideoPlayerEvent.value).isEqualTo(consumed())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
    ) = VideoUiEntity(
        id = NodeId(handle),
        name = name,
        parentId = NodeId(parentHandle),
        elementID = 1L,
        duration = 1.minutes,
        isSharedItems = false,
        size = 100L,
        fileTypeInfo = VideoFileTypeInfo("video", "mp4", 1.minutes),
        isMarkedSensitive = false,
        isSensitiveInherited = false,
        locations = listOf(LocationFilterOption.AllLocations)
    )

    private fun stubInitialValues(hiddenEnabled: Boolean = true, showHidden: Boolean = true) {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(
            flow {
                emit(NodeUpdate(emptyMap()))
                awaitCancellation()
            }
        )
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flow {
                emit(emptyList())
                awaitCancellation()
            }
        )
        runBlocking {
            whenever(monitorVideoRecentlyWatchedUseCase()).thenReturn(
                flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            )
        }
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(
            flow {
                emit(hiddenEnabled)
                awaitCancellation()
            }
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(
            flow {
                emit(showHidden)
                awaitCancellation()
            }
        )
    }
}