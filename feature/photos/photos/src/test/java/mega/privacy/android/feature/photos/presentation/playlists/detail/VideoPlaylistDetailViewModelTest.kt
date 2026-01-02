package mega.privacy.android.feature.photos.presentation.playlists.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistByIdUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistDetailViewModelTest {
    private lateinit var underTest: VideoPlaylistDetailViewModel

    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorVideoPlaylistSetsUpdateUseCase =
        mock<MonitorVideoPlaylistSetsUpdateUseCase>()
    private val videoPlaylistUiEntityMapper = mock<VideoPlaylistUiEntityMapper>()
    private val getVideoPlaylistByIdUseCase = mock<GetVideoPlaylistByIdUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testId = NodeId(123456L)
    private val testType = PlaylistType.User
    private val expectedPlaylist = mock<VideoPlaylistUiEntity> {
        on { id }.thenReturn(testId)
        on { title }.thenReturn("Playlist 1")
    }

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(monitorNodeUpdatesUseCase()).thenReturn(
                flow {
                    emit(NodeUpdate(emptyMap()))
                    awaitCancellation()
                }
            )
            whenever(monitorVideoPlaylistSetsUpdateUseCase()).thenReturn(
                flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            )
            whenever(videoPlaylistUiEntityMapper(any())).thenReturn(expectedPlaylist)
            whenever(getVideoPlaylistByIdUseCase(any(), any())).thenReturn(mock())
        }
    }

    private fun initViewModel(
        playlistHandle: Long = testId.longValue,
        type: PlaylistType = testType,
    ) {
        underTest = VideoPlaylistDetailViewModel(
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase = monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistUiEntityMapper = videoPlaylistUiEntityMapper,
            getVideoPlaylistByIdUseCase = getVideoPlaylistByIdUseCase,
            playlistHandle = playlistHandle,
            type = type
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistUiEntityMapper,
            getVideoPlaylistByIdUseCase
        )
    }

    @BeforeAll
    fun init() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that uiState is correctly updated when monitorVideoPlaylistSetsUpdateUseCase is triggered`() =
        runTest {
            val playlistsUpdatesFlow = MutableSharedFlow<List<Long>>()
            whenever(monitorVideoPlaylistSetsUpdateUseCase()).thenReturn(playlistsUpdatesFlow)
            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem().let { it as VideoPlaylistDetailUiState.Data }
                assertThat(actual.currentPlaylist).isNotNull()
                assertThat(actual.currentPlaylist).isEqualTo(expectedPlaylist)
                cancelAndIgnoreRemainingEvents()
                playlistsUpdatesFlow.emit(listOf(testId.longValue))

                verify(getVideoPlaylistByIdUseCase, times(2)).invoke(testId, testType)
            }
        }

    @Test
    fun `test that uiState is not updated when monitorVideoPlaylistSetsUpdateUseCase triggered not the current playlist`() =
        runTest {
            val playlistsUpdatesFlow = MutableSharedFlow<List<Long>>()
            whenever(monitorVideoPlaylistSetsUpdateUseCase()).thenReturn(playlistsUpdatesFlow)
            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                    .let { it as VideoPlaylistDetailUiState.Data }
                assertThat(actual.currentPlaylist).isNotNull()
                assertThat(actual.currentPlaylist).isEqualTo(expectedPlaylist)
                cancelAndIgnoreRemainingEvents()
                playlistsUpdatesFlow.emit(listOf(100L))

                verify(getVideoPlaylistByIdUseCase, times(1)).invoke(testId, testType)
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            val testVideoId = NodeId(654321L)
            val testFileNode = mock<FileNode> {
                on { id }.thenReturn(testVideoId)
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            val testVideo = mock<VideoUiEntity> {
                on { id }.thenReturn(testVideoId)
            }
            val testPlaylist = mock<VideoPlaylistUiEntity> {
                on { id }.thenReturn(testId)
                on { title }.thenReturn("Playlist 1")
                on { videos }.thenReturn(listOf(testVideo))
            }
            whenever(videoPlaylistUiEntityMapper(any())).thenReturn(testPlaylist)
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)

            initViewModel()

            underTest.uiState.test {
                val actual = awaitItem()
                    .let { it as VideoPlaylistDetailUiState.Data }
                assertThat(actual.currentPlaylist).isNotNull()
                assertThat(actual.currentPlaylist).isEqualTo(testPlaylist)
                cancelAndIgnoreRemainingEvents()
                nodeUpdatesFlow.emit(
                    NodeUpdate(mapOf(testFileNode to emptyList()))
                )

                verify(getVideoPlaylistByIdUseCase, times(2)).invoke(testId, testType)
            }
        }

    @Test
    fun `test that uiState is not updated when updated nodes are not video type`() =
        runTest {
            val testVideoId = NodeId(654321L)
            val testFileNode = mock<FileNode> {
                on { id }.thenReturn(testVideoId)
                on { type }.thenReturn(TextFileTypeInfo("text", "txt"))
            }
            val testVideo = mock<VideoUiEntity> {
                on { id }.thenReturn(testVideoId)
            }
            val testPlaylist = mock<VideoPlaylistUiEntity> {
                on { id }.thenReturn(testId)
                on { title }.thenReturn("Playlist 1")
                on { videos }.thenReturn(listOf(testVideo))
            }
            whenever(videoPlaylistUiEntityMapper(any())).thenReturn(testPlaylist)
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)

            initViewModel()

            underTest.uiState.test {
                val actual = awaitItem()
                    .let { it as VideoPlaylistDetailUiState.Data }
                assertThat(actual.currentPlaylist).isNotNull()
                assertThat(actual.currentPlaylist).isEqualTo(testPlaylist)
                cancelAndIgnoreRemainingEvents()
                nodeUpdatesFlow.emit(
                    NodeUpdate(mapOf(testFileNode to emptyList()))
                )

                verify(getVideoPlaylistByIdUseCase, times(1)).invoke(testId, testType)
            }
        }

    @Test
    fun `test that uiState is not updated when videos do not include the updated nodes`() =
        runTest {
            val testVideoId = NodeId(654321L)
            val testFileNode = mock<FileNode> {
                on { id }.thenReturn(testVideoId)
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            val testPlaylist = mock<VideoPlaylistUiEntity> {
                on { id }.thenReturn(testId)
                on { title }.thenReturn("Playlist 1")
                on { videos }.thenReturn(emptyList())
            }
            whenever(videoPlaylistUiEntityMapper(any())).thenReturn(testPlaylist)
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)

            initViewModel()

            underTest.uiState.test {
                val actual = awaitItem()
                    .let { it as VideoPlaylistDetailUiState.Data }
                assertThat(actual.currentPlaylist).isNotNull()
                assertThat(actual.currentPlaylist).isEqualTo(testPlaylist)
                cancelAndIgnoreRemainingEvents()
                nodeUpdatesFlow.emit(
                    NodeUpdate(mapOf(testFileNode to emptyList()))
                )

                verify(getVideoPlaylistByIdUseCase, times(1)).invoke(testId, testType)
            }
        }
}