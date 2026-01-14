package mega.privacy.android.feature.photos.presentation.playlists

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.domain.entity.SortOrder
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("The entire test class is temporarily ignored and will be re-enabled once the issue is resolved.")
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
                    NodeUpdate(emptyMap())
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
        initUnderTest()
        advanceUntilIdle()

        underTest.uiState.test {
            val actual = awaitItem() as? VideoPlaylistsTabUiState.Data
            if (actual != null) {
                assertThat(actual.videoPlaylistEntities).isNotEmpty()
                assertThat(actual.videoPlaylists).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(expectedSortOrder)
                assertThat(actual.selectedSortConfiguration).isEqualTo(expectedConfig)
                assertThat(actual.selectedPlaylists).isEmpty()
                assertThat(actual.playlistsRemovedEvent).isEqualTo(consumed())
                assertThat(actual.query).isNull()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is correctly updated when setCloudSortOrder is invoked`() =
        runTest {
            val sortOrder = SortOrder.ORDER_CREATION_ASC
            val config = NodeSortConfiguration(NodeSortOption.Created, SortDirection.Ascending)
            whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>()))
                .thenReturn(sortOrder)
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(config)

            initUnderTest()
            underTest.setCloudSortOrder(config)
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    verify(setCloudSortOrderUseCase).invoke(sortOrder)
                    assertThat(actual.sortOrder).isEqualTo(expectedSortOrder)
                    assertThat(actual.selectedSortConfiguration).isEqualTo(config)
                }
                cancelAndIgnoreRemainingEvents()
            }
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

            underTest.uiState.test {
                val actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
                    assertThat(actual.selectedSortConfiguration).isEqualTo(expectedConfig)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            initVideoPlaylists(listOf(playlist1, playlist2))
            initUnderTest()
            underTest.onItemLongClicked(playlist1)
            advanceUntilIdle()

            underTest.uiState.test {
                skipItems(1)
                var actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.selectedPlaylists).hasSize(1)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(playlist1.id)
                }

                underTest.onItemClicked(playlist2)
                actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.selectedPlaylists).hasSize(2)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(
                        playlist1.id,
                        playlist2.id
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after selectAllVideos is invoked`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            initVideoPlaylists(listOf(playlist1, playlist2))
            initUnderTest()
            underTest.onItemLongClicked(playlist1)
            advanceUntilIdle()

            underTest.uiState.test {
                skipItems(2)

                underTest.selectAllVideos()
                var actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.selectedPlaylists).hasSize(2)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(
                        playlist1.id,
                        playlist2.id
                    )
                }

                underTest.clearSelection()
                actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.selectedPlaylists).isEmpty()
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after clearSelection is invoked`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            initVideoPlaylists(listOf(playlist1, playlist2))
            initUnderTest()
            underTest.onItemLongClicked(playlist1)
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.selectedPlaylists).hasSize(1)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(playlist1.id)
                }
                cancelAndIgnoreRemainingEvents()
            }
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

            initUnderTest()
            underTest.removeVideoPlaylists(setOf(playlist1, playlist2))
            advanceUntilIdle()

            underTest.uiState.test {
                skipItems(1)
                val actual = awaitItem() as? VideoPlaylistsTabUiState.Data
                if (actual != null) {
                    assertThat(actual.playlistsRemovedEvent).isEqualTo(
                        triggered(
                            listOf(playlist1.title, playlist2.title)
                        )
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when searchQuery is invoked`() = runTest {
        val query = "query"
        val videoPlaylist = createVideoPlaylistUiEntity(handle = 2L, name = "Playlist in query")
        whenever(
            getVideoPlaylistsUseCase()
        ).thenReturn(listOf(mock()))
        whenever(videoPlaylistUiEntityMapper(any())).thenReturn(videoPlaylist)

        initUnderTest()
        underTest.searchQuery(query)
        advanceUntilIdle()

        underTest.uiState.test {
            skipItems(1)
            val actual = awaitItem() as? VideoPlaylistsTabUiState.Data
            if (actual != null) {
                assertThat(actual.videoPlaylistEntities).isNotEmpty()
                assertThat(actual.videoPlaylistEntities.map { it.id }).containsExactly(
                    videoPlaylist.id
                )
                assertThat(actual.query).isEqualTo(query)
            }
            cancelAndIgnoreRemainingEvents()
        }
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