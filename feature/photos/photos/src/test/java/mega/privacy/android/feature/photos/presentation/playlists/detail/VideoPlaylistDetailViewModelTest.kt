package mega.privacy.android.feature.photos.presentation.playlists.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistByIdUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistDetailUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class VideoPlaylistDetailViewModelTest {
    private lateinit var underTest: VideoPlaylistDetailViewModel

    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorVideoPlaylistSetsUpdateUseCase =
        mock<MonitorVideoPlaylistSetsUpdateUseCase>()
    private val videoPlaylistDetailUiEntityMapper = mock<VideoPlaylistDetailUiEntityMapper>()
    private val getVideoPlaylistByIdUseCase = mock<GetVideoPlaylistByIdUseCase>()

    private val testId = NodeId(123456L)
    private val testType = PlaylistType.User
    private val expectedPlaylistUiEntity = createVideoPlaylistUiEntity(testId.longValue)
    private val expectedVideoUiEntities = (0..3).map {
        createVideoUiEntity(handle = it.toLong())
    }
    private val expectedVideoPlaylist = createVideoPlaylist(expectedPlaylistUiEntity)
    private val expectedPlaylistDetail = mock<VideoPlaylistDetailUiEntity> {
        on { uiEntity }.thenReturn(expectedPlaylistUiEntity)
        on { videos }.thenReturn(expectedVideoUiEntities)
    }

    @BeforeEach
    fun setUp() {
        underTest = VideoPlaylistDetailViewModel(
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase = monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistDetailUiEntityMapper = videoPlaylistDetailUiEntityMapper,
            getVideoPlaylistByIdUseCase = getVideoPlaylistByIdUseCase,
            playlistHandle = testId.longValue,
            type = testType
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistDetailUiEntityMapper,
            getVideoPlaylistByIdUseCase
        )
    }

    @Test
    fun `test that Data state is returned if values found`() = runTest {
        stubInitialValues()
        underTest.uiState
            .filterIsInstance<VideoPlaylistDetailUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.playlistDetail).isNotNull()
                assertThat(actual.playlistDetail?.uiEntity).isEqualTo(expectedPlaylistUiEntity)
                assertThat(actual.playlistDetail?.videos).isNotEmpty()
                assertThat(actual.playlistDetail?.videos).hasSize(4)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that getVideoPlaylistByIdUseCase is invoked when monitorVideoPlaylistSetsUpdateUseCase is triggered`() =
        runTest {
            stubInitialValues()
            val playlistsUpdatesFlow = MutableSharedFlow<List<Long>>()
            monitorVideoPlaylistSetsUpdateUseCase.stub {
                on { invoke() }.thenReturn(playlistsUpdatesFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.playlistDetail).isNotNull()
                    assertThat(initial.playlistDetail?.uiEntity).isEqualTo(expectedPlaylistUiEntity)
                    clearInvocations(getVideoPlaylistByIdUseCase)

                    playlistsUpdatesFlow.emit(listOf(testId.longValue))
                    verify(getVideoPlaylistByIdUseCase).invoke(testId, testType)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getVideoPlaylistByIdUseCase should not be invoked when the updated playlist is not current playlist`() =
        runTest {
            stubInitialValues()
            val playlistsUpdatesFlow = MutableSharedFlow<List<Long>>()
            monitorVideoPlaylistSetsUpdateUseCase.stub {
                on { invoke() }.thenReturn(playlistsUpdatesFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    skipItems(1)
                    clearInvocations(getVideoPlaylistByIdUseCase)

                    playlistsUpdatesFlow.emit(listOf(100L))
                    verifyNoInteractions(getVideoPlaylistByIdUseCase)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getVideoPlaylistByIdUseCase is invoked when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            val testNode = mock<FileNode> {
                on { id }.thenReturn(expectedVideoUiEntities.first().id)
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 1.minutes))
            }
            stubInitialValues()
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))

            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.playlistDetail).isNotNull()
                    assertThat(initial.playlistDetail?.uiEntity).isEqualTo(expectedPlaylistUiEntity)
                    assertThat(initial.playlistDetail?.videos).isNotEmpty()
                    assertThat(initial.playlistDetail?.videos).hasSize(4)
                    clearInvocations(getVideoPlaylistByIdUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verify(getVideoPlaylistByIdUseCase).invoke(any(), any())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getVideoPlaylistByIdUseCase should not be invoked when update nodes are not video type`() =
        runTest {
            val testNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(TextFileTypeInfo("document", "txt"))
            }
            stubInitialValues()
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))

            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    skipItems(1)
                    clearInvocations(getVideoPlaylistByIdUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verifyNoInteractions(getVideoPlaylistByIdUseCase)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getVideoPlaylistByIdUseCase should not be invoked when current playlist's videos do not include the updated nodes`() =
        runTest {
            val testNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(100L))
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 1.minutes))
            }
            stubInitialValues()
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))

            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    skipItems(1)
                    clearInvocations(getVideoPlaylistByIdUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verifyNoInteractions(getVideoPlaylistByIdUseCase)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    private suspend fun stubInitialValues(
        videoPlaylist: VideoPlaylist = expectedVideoPlaylist,
        detailEntity: VideoPlaylistDetailUiEntity = expectedPlaylistDetail,
    ) {
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
        whenever(getVideoPlaylistByIdUseCase(any(), any())).thenReturn(videoPlaylist)
        whenever(videoPlaylistDetailUiEntityMapper(videoPlaylist)).thenReturn(detailEntity)
    }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
        duration: Duration = 1.minutes,
        isSharedItems: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = VideoUiEntity(
        id = NodeId(handle),
        name = name,
        parentId = NodeId(parentHandle),
        elementID = 1L,
        duration = duration,
        isSharedItems = isSharedItems,
        size = 100L,
        fileTypeInfo = VideoFileTypeInfo("video", "mp4", duration),
        isMarkedSensitive = isMarkedSensitive,
        isSensitiveInherited = isSensitiveInherited,
        locations = listOf(LocationFilterOption.AllLocations)
    )

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
        isSelected = isSelected,
        isSystemVideoPlayer = isSystemVideoPlayer
    )

    private fun createVideoPlaylist(
        playlistUiEntity: VideoPlaylistUiEntity
    ) =
        UserVideoPlaylist(
            id = playlistUiEntity.id,
            title = playlistUiEntity.title,
            cover = null,
            creationTime = playlistUiEntity.creationTime,
            modificationTime = playlistUiEntity.modificationTime,
            thumbnailList = playlistUiEntity.thumbnailList,
            numberOfVideos = playlistUiEntity.numberOfVideos,
            totalDuration = 0.minutes,
            videos = emptyList()
        )
}