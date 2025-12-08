package mega.privacy.android.feature.photos.presentation.videos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideosTabViewModelTest {
    private lateinit var underTest: VideosTabViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val videoUiEntityMapper = mock<VideoUiEntityMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val nodeSortConfigurationUiMapper = mock<NodeSortConfigurationUiMapper>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()

    private val expectedId = NodeId(1L)
    private val expectedVideo = mock<VideoUiEntity> {
        on { id }.thenReturn(expectedId)
        on { name }.thenReturn("video name")
        on { elementID }.thenReturn(1L)
    }

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(monitorNodeUpdatesUseCase()).thenReturn(
                flow {
                    emptyMap<FileNode, NodeUpdate>()
                    awaitCancellation()
                }
            )
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
                flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            )

            whenever(monitorSortCloudOrderUseCase()).thenReturn(
                flow {
                    emit(null)
                    awaitCancellation()
                }
            )

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(
                getAllVideosUseCase(
                    searchQuery = anyOrNull(),
                    tag = anyOrNull(),
                    description = anyOrNull()
                )
            ).thenReturn(listOf(mock(), mock()))
            whenever(videoUiEntityMapper(any())).thenReturn(expectedVideo)
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(
                NodeSortConfiguration.default
            )
        }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideosTabViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            videoUiEntityMapper = videoUiEntityMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            videoUiEntityMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase
        )
    }

    @Test
    fun `test that the initial state is correctly updated`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is correctly updated triggerRefresh is invoked`() =
        runTest {
            underTest.triggerRefresh()

            underTest.uiState.drop(1).test {
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideos).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideos.size).isEqualTo(2)
                assertThat(actual.query).isNull()
                assertThat(actual.highlightText).isEmpty()
                assertThat(actual.selectedSortConfiguration).isEqualTo(NodeSortConfiguration.default)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            val testFileNode = mock<FileNode> {
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(NodeUpdate(mapOf(testFileNode to emptyList())))
                        awaitCancellation()
                    }
                )
            }

            underTest.uiState.drop(1).test {
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideos).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideos.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorNodeUpdatesUseCase is triggered but changed node is not videoType`() =
        runTest {
            val testFileNode = mock<FileNode> {
                on { type }.thenReturn(TextFileTypeInfo("TextFile", "txt"))
            }
            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(NodeUpdate(mapOf(testFileNode to emptyList())))
                        awaitCancellation()
                    }
                )
            }

            underTest.uiState.test {
                assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            val testOffline = mock<Offline>()
            monitorOfflineNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(listOf(testOffline))
                        awaitCancellation()
                    }
                )
            }

            underTest.uiState.drop(1).test {
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideos).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideos.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when searchQuery is invoked`() = runTest {
        val query = "query"
        val video = createVideoUiEntity(handle = 2L, name = "video file in query")
        val typedNode = mock<TypedVideoNode>()
        whenever(
            getAllVideosUseCase(
                searchQuery = query,
                tag = query.removePrefix("#"),
                description = query
            )
        ).thenReturn(listOf(typedNode))
        whenever(videoUiEntityMapper(typedNode)).thenReturn(video)
        underTest.searchQuery(query)

        underTest.uiState.drop(1).test {
            val actual = awaitItem() as VideosTabUiState.Data
            assertThat(actual.allVideos).isNotEmpty()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allVideos.size).isEqualTo(1)
            assertThat(actual.query).isEqualTo(query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
        duration: Duration = 1.minutes,
        isSharedItems: Boolean = false,
    ) = mock<VideoUiEntity> {
        on { id }.thenReturn(NodeId(handle))
        on { parentId }.thenReturn(NodeId(parentHandle))
        on { this.name }.thenReturn(name)
        on { elementID }.thenReturn(1L)
        on { this.duration }.thenReturn(duration)
        on { this.isSharedItems }.thenReturn(isSharedItems)
    }

    @Test
    fun `test that uiState is correctly updated when setCloudSortOrder is invoked`() =
        runTest {
            val sortOrder = SortOrder.ORDER_FAV_ASC
            whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>()))
                .thenReturn(sortOrder)
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(
                NodeSortConfiguration.default
            )

            underTest.setCloudSortOrder(NodeSortConfiguration.default)

            underTest.uiState.test {
                verify(setCloudSortOrderUseCase).invoke(any())
                assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideos).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideos.size).isEqualTo(2)
                assertThat(actual.selectedSortConfiguration).isEqualTo(NodeSortConfiguration.default)
                cancelAndIgnoreRemainingEvents()
            }
        }
}