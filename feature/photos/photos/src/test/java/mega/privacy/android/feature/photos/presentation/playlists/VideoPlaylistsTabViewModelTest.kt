package mega.privacy.android.feature.photos.presentation.playlists

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistsTabViewModelTest {
    private lateinit var underTest: VideoPlaylistsTabViewModel

    private val getVideoPlaylistsUseCase = mock<GetVideoPlaylistsUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorVideoPlaylistSetsUpdateUseCase =
        mock<MonitorVideoPlaylistSetsUpdateUseCase>()
    private val videoPlaylistUiEntityMapper = mock<VideoPlaylistUiEntityMapper>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val nodeSortConfigurationUiMapper = mock<NodeSortConfigurationUiMapper>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val removeVideoPlaylistsUseCase = mock<RemoveVideoPlaylistsUseCase>()

    private val expectedId = NodeId(1L)
    private val expectedPlaylist = mock<VideoPlaylistUiEntity> {
        on { id }.thenReturn(expectedId)
        on { title }.thenReturn("Playlist 1")
    }

    private val expectedSortOrder = SortOrder.ORDER_MODIFICATION_DESC
    private val expectedConfig = NodeSortConfiguration.default

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(monitorNodeUpdatesUseCase()).thenReturn(
                flow {
                    emptyMap<FileNode, NodeUpdate>()
                    awaitCancellation()
                }
            )
            whenever(monitorVideoPlaylistSetsUpdateUseCase()).thenReturn(
                flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            )
            whenever(monitorSortCloudOrderUseCase()).thenReturn(
                flow {
                    emit(expectedSortOrder)
                    awaitCancellation()
                }
            )
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(expectedConfig)
            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(mock(), mock()))
            whenever(videoPlaylistUiEntityMapper(any())).thenReturn(expectedPlaylist)
        }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoPlaylistsTabViewModel(
            getVideoPlaylistsUseCase = getVideoPlaylistsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase = monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistUiEntityMapper = videoPlaylistUiEntityMapper,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            removeVideoPlaylistsUseCase = removeVideoPlaylistsUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getVideoPlaylistsUseCase,
            monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistUiEntityMapper,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase,
            removeVideoPlaylistsUseCase,
        )
    }

    @Test
    fun `test that the initial state is correctly updated`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideoPlaylistsTabUiState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is correctly updated triggerRefresh is invoked`() =
        runTest {
            underTest.triggerRefresh()
            advanceUntilIdle()

            val actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.videoPlaylistEntities.isNotEmpty() }

            assertThat(actual.videoPlaylistEntities).isNotEmpty()
            assertThat(actual.videoPlaylistEntities).hasSize(2)
            assertThat(actual.sortOrder).isEqualTo(expectedSortOrder)
            assertThat(actual.selectedSortConfiguration).isEqualTo(expectedConfig)
        }

    @Test
    fun `test that uiState is correctly updated when monitorVideoPlaylistSetsUpdateUseCase is triggered`() =
        runTest {
            monitorVideoPlaylistSetsUpdateUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(listOf(1L, 2L))
                        awaitCancellation()
                    }
                )
            }
            initUnderTest()
            val actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.videoPlaylistEntities.isNotEmpty() }

            assertThat(actual.videoPlaylistEntities).isNotEmpty()
            assertThat(actual.videoPlaylistEntities).hasSize(2)
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

            val actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.videoPlaylistEntities.isNotEmpty() }

            assertThat(actual.videoPlaylistEntities).isNotEmpty()
            assertThat(actual.videoPlaylistEntities).hasSize(2)
        }

    @Test
    fun `test that uiState is correctly updated when monitorNodeUpdatesUseCase is triggered but changed node is not videoType`() =
        runTest {
            val testFileNode = mock<FileNode> {
                on { type }.thenReturn(TextFileTypeInfo("TextFile", "txt"))
            }

            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdatesFlow)
            }
            initUnderTest()

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first()
            advanceUntilIdle()

            resetMocks()
            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(mock(), mock()))

            nodeUpdatesFlow.emit(NodeUpdate(mapOf(testFileNode to emptyList())))
            advanceUntilIdle()

            // Verify that getVideoPlaylistsUseCase was NOT called because the update was filtered out
            verifyNoMoreInteractions(getVideoPlaylistsUseCase)
        }

    @Test
    fun `test that uiState is correctly updated when setCloudSortOrder is invoked`() =
        runTest {
            val sortOrder = SortOrder.ORDER_CREATION_ASC
            val config = NodeSortConfiguration(NodeSortOption.Created, SortDirection.Ascending)
            whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>()))
                .thenReturn(sortOrder)
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(config)

            underTest.setCloudSortOrder(config)
            advanceUntilIdle()

            val actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.videoPlaylistEntities.isNotEmpty() }

            verify(setCloudSortOrderUseCase).invoke(sortOrder)
            assertThat(actual.sortOrder).isEqualTo(expectedSortOrder)
            assertThat(actual.selectedSortConfiguration).isEqualTo(config)
        }

    @ParameterizedTest(name = "when sortOrder is {0}")
    @EnumSource(
        value = SortOrder::class,
        names = ["ORDER_LABEL_DESC", "ORDER_LABEL_ASC", "ORDER_FAV_DESC", "ORDER_FAV_ASC", "ORDER_SIZE_DESC", "ORDER_SIZE_ASC"]
    )
    fun `test that sortOrder is ORDER_DEFAULT_ASC`(
        sortOrder: SortOrder,
    ) =
        runTest {
            whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>()))
                .thenReturn(sortOrder)
            whenever(monitorSortCloudOrderUseCase()).thenReturn(
                flow {
                    emit(sortOrder)
                    awaitCancellation()
                }
            )
            whenever(
                nodeSortConfigurationUiMapper(any(), any())
            ).thenReturn(expectedConfig)
            initUnderTest()
            advanceUntilIdle()

            val actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.videoPlaylistEntities.isNotEmpty() }

            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
            assertThat(actual.selectedSortConfiguration).isEqualTo(expectedConfig)
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            initVideoPlaylists(listOf(playlist1, playlist2))
            underTest.onItemLongClicked(playlist1)

            var actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.selectedPlaylists.size == 1 }

            assertThat(actual.selectedPlaylists).hasSize(1)
            assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(playlist1.id)

            underTest.onItemClicked(playlist2)

            actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.selectedPlaylists.size == 2 }

            assertThat(actual.selectedPlaylists).hasSize(2)
            assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(
                playlist1.id,
                playlist2.id
            )
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after selectAllVideos is invoked`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            initVideoPlaylists(listOf(playlist1, playlist2))

            var actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.videoPlaylistEntities.isNotEmpty() }
            assertThat(actual.videoPlaylistEntities).isNotEmpty()
            assertThat(actual.selectedPlaylists).isEmpty()

            underTest.selectAllVideos()

            actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.selectedPlaylists.isNotEmpty() }

            assertThat(actual.selectedPlaylists).hasSize(2)
            assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(
                playlist1.id,
                playlist2.id
            )
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after clearSelection is invoked`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            initVideoPlaylists(listOf(playlist1, playlist2))

            underTest.onItemLongClicked(playlist1)

            var actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.selectedPlaylists.size == 1 }

            assertThat(actual.selectedPlaylists).hasSize(1)
            assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(playlist1.id)

            underTest.clearSelection()

            actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.selectedPlaylists.isEmpty() }

            assertThat(actual.selectedPlaylists).isEmpty()
        }

    @Test
    fun `test that playlistsRemovedEvent is updated correctly`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L, name = "Playlist 1")
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L, name = "Playlist 2")

            initVideoPlaylists(listOf(playlist1, playlist2))

            whenever(
                removeVideoPlaylistsUseCase(any())
            ).thenReturn(listOf(playlist1.id.longValue, playlist2.id.longValue))

            underTest.removeVideoPlaylists(setOf(playlist1, playlist2))
            advanceUntilIdle()

            var actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.playlistsRemovedEvent != consumed() }

            assertThat(actual.playlistsRemovedEvent).isEqualTo(
                triggered(
                    listOf(playlist1.title, playlist2.title)
                )
            )

            underTest.resetPlaylistsRemovedEvent()

            actual = underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .first { it.playlistsRemovedEvent == consumed() }

            assertThat(actual.playlistsRemovedEvent).isEqualTo(consumed())
        }

    private fun createVideoPlaylistUiEntity(
        handle: Long,
        name: String = "video name $handle",
        isSelected: Boolean = false,
        isSystemVideoPlayer: Boolean = false,
    ) = VideoPlaylistUiEntity(
        id = NodeId(handle),
        title = name,
        cover = null,
        creationTime = 1L,
        modificationTime = 1L,
        thumbnailList = emptyList(),
        numberOfVideos = 0,
        totalDuration = "",
        videos = emptyList(),
        isSelected = isSelected,
        isSystemVideoPlayer = isSystemVideoPlayer
    )

    private fun createVideoPlaylist(handle: Long) =
        mock<UserVideoPlaylist> {
            on { id }.thenReturn(NodeId(handle))
            on { title }.thenReturn("Playlist $handle")
        }

    private suspend fun initVideoPlaylists(
        playlists: List<VideoPlaylistUiEntity>,
    ) {
        val userPlaylists = playlists.map {
            createVideoPlaylist(it.id.longValue)
        }
        whenever(getVideoPlaylistsUseCase()).thenReturn(userPlaylists)

        userPlaylists.map { playlist ->
            playlists.firstOrNull { it.id.longValue == playlist.id.longValue }
                ?.let { playlistEntity ->
                    whenever(videoPlaylistUiEntityMapper(playlist)).thenReturn(playlistEntity)
                }
        }
    }
}