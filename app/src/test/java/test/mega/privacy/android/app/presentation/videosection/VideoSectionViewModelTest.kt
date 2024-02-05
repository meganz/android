package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoPlaylistMapper
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.presentation.videosection.model.UIVideoPlaylist
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoSectionViewModelTest {
    private lateinit var underTest: VideoSectionViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val uiVideoMapper = mock<UIVideoMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val fakeMonitorNodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val fakeMonitorOfflineNodeUpdatesFlow = MutableSharedFlow<List<Offline>>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getVideoPlaylistsUseCase = mock<GetVideoPlaylistsUseCase>()
    private val uiVideoPlaylistMapper = mock<UIVideoPlaylistMapper>()
    private val createVideoPlaylistUseCase = mock<CreateVideoPlaylistUseCase>()
    private val addVideosToPlaylistUseCase = mock<AddVideosToPlaylistUseCase>()

    private val expectedVideo = mock<UIVideo> { on { name }.thenReturn("video name") }

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(fakeMonitorNodeUpdatesFlow)
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(
            fakeMonitorOfflineNodeUpdatesFlow
        )
        wheneverBlocking { getVideoPlaylistsUseCase() }.thenReturn(listOf())
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoSectionViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            uiVideoMapper = uiVideoMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getNodeByHandle = getNodeByHandle,
            getFingerprintUseCase = getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase = getFileUrlByNodeHandleUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getVideoPlaylistsUseCase = getVideoPlaylistsUseCase,
            uiVideoPlaylistMapper = uiVideoPlaylistMapper,
            createVideoPlaylistUseCase = createVideoPlaylistUseCase,
            addVideosToPlaylistUseCase = addVideosToPlaylistUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            uiVideoMapper,
            getCloudSortOrder,
            getNodeByHandle,
            getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase,
            getNodeByIdUseCase,
            getVideoPlaylistsUseCase,
            uiVideoPlaylistMapper,
            createVideoPlaylistUseCase,
            addVideosToPlaylistUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.allVideos).isEmpty()
            assertThat(initial.isPendingRefresh).isFalse()
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.progressBarShowing).isEqualTo(true)
            assertThat(initial.videoPlaylists).isEmpty()
            assertThat(initial.currentVideoPlaylist).isNull()
            assertThat(initial.isVideoPlaylistCreatedSuccessfully).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the videos are retrieved when the nodes are refreshed`() = runTest {
        initVideosReturned()

        underTest.refreshNodes()

        underTest.state.drop(1).test {
            val actual = awaitItem()
            assertThat(actual.allVideos).isNotEmpty()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allVideos.size).isEqualTo(2)
            assertThat(actual.progressBarShowing).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun initVideosReturned() {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllVideosUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(uiVideoMapper(any())).thenReturn(expectedVideo)
    }

    @Test
    fun `test that isPendingRefresh is correctly updated when monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            testScheduler.advanceUntilIdle()

            underTest.state.drop(1).test {
                fakeMonitorOfflineNodeUpdatesFlow.emit(emptyList())
                assertThat(awaitItem().isPendingRefresh).isTrue()

                underTest.markHandledPendingRefresh()
                assertThat(awaitItem().isPendingRefresh).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isPendingRefresh is correctly updated when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            testScheduler.advanceUntilIdle()

            underTest.state.drop(1).test {
                fakeMonitorNodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
                assertThat(awaitItem().isPendingRefresh).isTrue()

                underTest.markHandledPendingRefresh()
                assertThat(awaitItem().isPendingRefresh).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the sortOrder is updated when order is changed`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllVideosUseCase()).thenReturn(emptyList())

        underTest.refreshWhenOrderChanged()

        underTest.state.drop(1).test {
            assertThat(awaitItem().sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the videos returned correctly when search query is not empty`() = runTest {
        val expectedTypedVideoNode = mock<TypedVideoNode> { on { name }.thenReturn("video name") }
        val videoNode = mock<TypedVideoNode> { on { name }.thenReturn("name") }
        val expectedVideo = mock<UIVideo> { on { name }.thenReturn("video name") }
        val video = mock<UIVideo> { on { name }.thenReturn("name") }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllVideosUseCase()).thenReturn(listOf(expectedTypedVideoNode, videoNode))
        whenever(uiVideoMapper(expectedTypedVideoNode)).thenReturn(expectedVideo)
        whenever(uiVideoMapper(videoNode)).thenReturn(video)

        underTest.refreshNodes()

        underTest.state.drop(1).test {
            assertThat(awaitItem().allVideos.size).isEqualTo(2)

            underTest.searchQuery("video")
            assertThat(awaitItem().allVideos.size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the searchMode is correctly updated`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllVideosUseCase()).thenReturn(emptyList())

        underTest.state.drop(1).test {
            underTest.searchReady()
            assertThat(awaitItem().searchMode).isTrue()

            underTest.exitSearch()
            assertThat(awaitItem().searchMode).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the selected item is updated by 1 when long clicked`() =
        runTest {
            initVideosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos).isNotEmpty()

                underTest.onLongItemClicked(expectedVideo, 0)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the checked index is incremented by 1 when the selected item gets clicked`() =
        runTest {
            initVideosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos.size).isEqualTo(2)

                underTest.onLongItemClicked(expectedVideo, 0)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(1)

                underTest.onItemClicked(expectedVideo, 1)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the selected videos size equals the videos size when selecting all videos`() =
        runTest {
            initVideosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos.size).isEqualTo(2)

                underTest.selectAllNodes()
                awaitItem().let { state ->
                    assertThat(state.selectedVideoHandles.size).isEqualTo(state.allVideos.size)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isInSelection is correctly updated when selecting and clearing all nodes`() =
        runTest {
            initVideosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()

                underTest.selectAllNodes()
                assertThat(awaitItem().isInSelection).isTrue()

                underTest.clearAllSelectedVideos()
                assertThat(awaitItem().isInSelection).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the playlists are returned correctly`() = runTest {
        initVideoPlaylistsReturned()
        initUnderTest()

        underTest.onTabSelected(VideoSectionTab.Playlists)

        underTest.state.drop(1).test {
            val actual = awaitItem()
            assertThat(actual.videoPlaylists.size).isEqualTo(2)
            assertThat(actual.isPlaylistProgressBarShown).isFalse()
        }
    }

    private suspend fun initVideoPlaylistsReturned() {
        val uiVideoPlaylist = mock<UIVideoPlaylist> {
            on { title }.thenReturn("playlist")
        }
        whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(uiVideoPlaylistMapper(any())).thenReturn(uiVideoPlaylist)
    }

    @Test
    fun `test that the playlists returned correctly when search query is not empty`() = runTest {
        val testTitle = "new playlist"
        val expectedVideoPlaylist = mock<VideoPlaylist> { on { title }.thenReturn(testTitle) }
        val videoPlaylist = mock<VideoPlaylist> { on { title }.thenReturn("title") }
        val expectedUIVideoPlaylist = mock<UIVideoPlaylist> { on { title }.thenReturn(testTitle) }
        val uiVideoPlaylist = mock<UIVideoPlaylist> { on { title }.thenReturn("title") }

        whenever(getVideoPlaylistsUseCase()).thenReturn(
            listOf(
                expectedVideoPlaylist,
                videoPlaylist
            )
        )
        whenever(uiVideoPlaylistMapper(expectedVideoPlaylist)).thenReturn(expectedUIVideoPlaylist)
        whenever(uiVideoPlaylistMapper(videoPlaylist)).thenReturn(uiVideoPlaylist)

        underTest.onTabSelected(selectTab = VideoSectionTab.Playlists)

        underTest.state.drop(1).test {
            assertThat(awaitItem().videoPlaylists.size).isEqualTo(2)

            underTest.searchQuery("playlist")
            assertThat(awaitItem().videoPlaylists.size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that create video playlist returns a video playlist with the right title`() =
        runTest {
            val expectedTitle = "video playlist title"
            val expectedVideoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn(expectedTitle)
            }
            whenever(createVideoPlaylistUseCase(expectedTitle)).thenReturn(expectedVideoPlaylist)

            initUnderTest()

            underTest.createNewPlaylist(expectedTitle)
            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.currentVideoPlaylist?.title).isEqualTo(expectedTitle)
                assertThat(actual.isVideoPlaylistCreatedSuccessfully).isTrue()
            }
        }

    @Test
    fun `test that the correct state is returned when an error occurs in creating the video playlist`() =
        runTest {
            whenever(createVideoPlaylistUseCase(any())).thenAnswer { throw Exception() }

            initUnderTest()

            underTest.createNewPlaylist("video playlist title")
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.currentVideoPlaylist).isNull()
                assertThat(actual.isVideoPlaylistCreatedSuccessfully).isFalse()
            }
        }

    @Test
    fun `test that the number of added videos is correct when adding videos to a playlist`() =
        runTest {
            val testPlaylistID = NodeId(1L)
            val testVideoIDs = listOf(NodeId(1L), NodeId(2L), NodeId(3L))
            whenever(addVideosToPlaylistUseCase(testPlaylistID, testVideoIDs)).thenReturn(
                testVideoIDs.size
            )

            initUnderTest()

            underTest.addVideosToPlaylist(testPlaylistID, testVideoIDs)
            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.numberOfAddedVideos).isEqualTo(testVideoIDs.size)
            }
        }

    @Test
    fun `test that the searchMode is closed when tab is switched`() = runTest {
        initVideoPlaylistsReturned()
        initVideosReturned()
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().searchMode).isFalse()
            underTest.searchReady()
            assertThat(awaitItem().searchMode).isTrue()
            underTest.onTabSelected(selectTab = VideoSectionTab.Playlists)
            assertThat(awaitItem().searchMode).isFalse()
            underTest.searchReady()
            assertThat(awaitItem().searchMode).isTrue()
            underTest.onTabSelected(selectTab = VideoSectionTab.All)
            assertThat(awaitItem().searchMode).isFalse()
        }
    }
}
