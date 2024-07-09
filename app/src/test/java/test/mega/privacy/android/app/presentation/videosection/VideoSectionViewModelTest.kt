package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistUIEntityMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideosFromPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoSectionViewModelTest {
    private lateinit var underTest: VideoSectionViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val videoUIEntityMapper = mock<VideoUIEntityMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val fakeMonitorNodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val fakeMonitorOfflineNodeUpdatesFlow = MutableSharedFlow<List<Offline>>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getVideoPlaylistsUseCase = mock<GetVideoPlaylistsUseCase>()
    private val videoPlaylistUIEntityMapper = mock<VideoPlaylistUIEntityMapper>()
    private val createVideoPlaylistUseCase = mock<CreateVideoPlaylistUseCase>()
    private val addVideosToPlaylistUseCase = mock<AddVideosToPlaylistUseCase>()
    private val getNextDefaultAlbumNameUseCase = mock<GetNextDefaultAlbumNameUseCase>()
    private val removeVideoPlaylistsUseCase = mock<RemoveVideoPlaylistsUseCase>()
    private val updateVideoPlaylistTitleUseCase = mock<UpdateVideoPlaylistTitleUseCase>()
    private val getSyncUploadsFolderIdsUseCase = mock<GetSyncUploadsFolderIdsUseCase>()
    private val removeVideosFromPlaylistUseCase = mock<RemoveVideosFromPlaylistUseCase>()
    private val monitorVideoPlaylistSetsUpdateUseCase =
        mock<MonitorVideoPlaylistSetsUpdateUseCase>()
    private val fakeMonitorVideoPlaylistSetsUpdateFlow = MutableSharedFlow<List<Long>>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        on {
            runBlocking { invoke() }
        }.thenReturn(false)
    }

    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase> {
        on {
            runBlocking { invoke() }
        }.thenReturn(flowOf(false))
    }
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val expectedId = NodeId(1)
    private val expectedVideo = mock<VideoUIEntity> {
        on { id }.thenReturn(expectedId)
        on { name }.thenReturn("video name")
        on { elementID }.thenReturn(1L)
    }
    private val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
        on { id }.thenReturn(expectedId)
        on { title }.thenReturn("playlist")
        on { videos }.thenReturn(listOf(expectedVideo, expectedVideo))
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(fakeMonitorNodeUpdatesFlow)
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(
            fakeMonitorOfflineNodeUpdatesFlow
        )
        wheneverBlocking { monitorVideoPlaylistSetsUpdateUseCase() }.thenReturn(
            fakeMonitorVideoPlaylistSetsUpdateFlow
        )
        wheneverBlocking { getVideoPlaylistsUseCase() }.thenReturn(listOf())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideoSectionViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            videoUIEntityMapper = videoUIEntityMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getNodeByHandle = getNodeByHandle,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getVideoPlaylistsUseCase = getVideoPlaylistsUseCase,
            videoPlaylistUIEntityMapper = videoPlaylistUIEntityMapper,
            createVideoPlaylistUseCase = createVideoPlaylistUseCase,
            addVideosToPlaylistUseCase = addVideosToPlaylistUseCase,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
            removeVideoPlaylistsUseCase = removeVideoPlaylistsUseCase,
            updateVideoPlaylistTitleUseCase = updateVideoPlaylistTitleUseCase,
            getSyncUploadsFolderIdsUseCase = getSyncUploadsFolderIdsUseCase,
            removeVideosFromPlaylistUseCase = removeVideosFromPlaylistUseCase,
            monitorVideoPlaylistSetsUpdateUseCase = monitorVideoPlaylistSetsUpdateUseCase,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getNodeContentUriUseCase = getNodeContentUriUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            videoUIEntityMapper,
            getCloudSortOrder,
            getNodeByHandle,
            getNodeByIdUseCase,
            getVideoPlaylistsUseCase,
            videoPlaylistUIEntityMapper,
            createVideoPlaylistUseCase,
            addVideosToPlaylistUseCase,
            getNextDefaultAlbumNameUseCase,
            updateVideoPlaylistTitleUseCase,
            getSyncUploadsFolderIdsUseCase,
            removeVideosFromPlaylistUseCase,
            monitorVideoPlaylistSetsUpdateUseCase,
            getNodeContentUriUseCase
        )
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
        whenever(videoUIEntityMapper(any())).thenReturn(expectedVideo)
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
        val expectedVideo = mock<VideoUIEntity> { on { name }.thenReturn("video name") }
        val video = mock<VideoUIEntity> { on { name }.thenReturn("name") }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllVideosUseCase()).thenReturn(listOf(expectedTypedVideoNode, videoNode))
        whenever(videoUIEntityMapper(expectedTypedVideoNode)).thenReturn(expectedVideo)
        whenever(videoUIEntityMapper(videoNode)).thenReturn(video)

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
            initUnderTest()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos).isNotEmpty()

                underTest.onItemLongClicked(expectedVideo, 0)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the checked index is incremented by 1 when the selected item gets clicked`() =
        runTest {
            initVideosReturned()
            initUnderTest()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos.size).isEqualTo(2)

                underTest.onItemLongClicked(expectedVideo, 0)
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
        whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(videoPlaylistUIEntityMapper(any())).thenReturn(videoPlaylistUIEntity)
    }

    @Test
    fun `test that the selected playlist item is updated by 1 when long clicked`() =
        runTest {
            initVideoPlaylistsReturned()
            initUnderTest()

            underTest.onTabSelected(VideoSectionTab.Playlists)

            underTest.state.drop(1).test {
                assertThat(awaitItem().videoPlaylists).isNotEmpty()

                underTest.onVideoPlaylistItemClicked(videoPlaylistUIEntity, 0)
                assertThat(awaitItem().selectedVideoPlaylistHandles.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the checked index is incremented by 1 when the selected playlist item gets clicked`() =
        runTest {
            initVideoPlaylistsReturned()
            initUnderTest()

            underTest.onTabSelected(VideoSectionTab.Playlists)

            underTest.state.drop(1).test {
                assertThat(awaitItem().videoPlaylists.size).isEqualTo(2)

                underTest.onVideoPlaylistItemClicked(videoPlaylistUIEntity, 0)
                assertThat(awaitItem().selectedVideoPlaylistHandles.size).isEqualTo(1)

                underTest.onVideoPlaylistItemClicked(videoPlaylistUIEntity, 1)
                assertThat(awaitItem().selectedVideoPlaylistHandles.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the selected playlist size equals the videos size when selecting all playlists`() =
        runTest {
            initVideoPlaylistsReturned()
            initUnderTest()

            underTest.onTabSelected(VideoSectionTab.Playlists)

            underTest.state.drop(1).test {
                assertThat(awaitItem().videoPlaylists.size).isEqualTo(2)

                underTest.selectAllVideoPlaylists()
                awaitItem().let { state ->
                    assertThat(state.selectedVideoPlaylistHandles.size).isEqualTo(state.videoPlaylists.size)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isInSelection is correctly updated when selecting and clearing all playlists`() =
        runTest {
            initVideoPlaylistsReturned()
            initUnderTest()

            underTest.state.drop(1).test {
                underTest.selectAllVideoPlaylists()
                assertThat(awaitItem().isInSelection).isTrue()

                underTest.clearAllSelectedVideoPlaylists()
                assertThat(awaitItem().isInSelection).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the playlists returned correctly when search query is not empty`() = runTest {
        val testTitle = "new playlist"
        val expectedVideoPlaylist = mock<VideoPlaylist> { on { title }.thenReturn(testTitle) }
        val videoPlaylist = mock<VideoPlaylist> { on { title }.thenReturn("title") }
        val expectedVideoPlaylistUIEntity =
            mock<VideoPlaylistUIEntity> { on { title }.thenReturn(testTitle) }
        val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> { on { title }.thenReturn("title") }

        whenever(getVideoPlaylistsUseCase()).thenReturn(
            listOf(
                expectedVideoPlaylist,
                videoPlaylist
            )
        )
        whenever(videoPlaylistUIEntityMapper(expectedVideoPlaylist)).thenReturn(
            expectedVideoPlaylistUIEntity
        )
        whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)

        underTest.onTabSelected(selectTab = VideoSectionTab.Playlists)

        underTest.state.drop(1).test {
            assertThat(awaitItem().videoPlaylists.size).isEqualTo(2)

            underTest.searchQuery("playlist")
            assertThat(awaitItem().videoPlaylists.size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the selected video item of playlist is updated by 1 when long clicked`() =
        runTest {
            initUnderTest()

            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)

            underTest.state.test {
                assertThat(awaitItem().currentVideoPlaylist?.videos).isNotEmpty()

                underTest.onVideoItemOfPlaylistLongClicked(expectedVideo, 0)
                assertThat(awaitItem().selectedVideoElementIDs.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the selected videos of playlist size equals the videos size when selecting all videos of playlist`() =
        runTest {
            initUnderTest()

            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)

            underTest.state.test {
                assertThat(awaitItem().currentVideoPlaylist?.videos?.size).isEqualTo(2)

                underTest.selectAllVideosOfPlaylist()
                awaitItem().let { state ->
                    assertThat(state.selectedVideoElementIDs.size).isEqualTo(2)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isInSelection is correctly updated when selecting and clearing all videos of playlist`() =
        runTest {
            initVideoPlaylistsReturned()
            initUnderTest()

            underTest.onTabSelected(VideoSectionTab.Playlists)

            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)

            underTest.state.drop(1).test {
                underTest.selectAllVideosOfPlaylist()
                assertThat(awaitItem().isInSelection).isTrue()

                underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)
                awaitItem()
                underTest.clearAllSelectedVideosOfPlaylist()
                assertThat(awaitItem().isInSelection).isFalse()
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
            val expectedVideoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { title }.thenReturn(expectedTitle)
            }
            whenever(createVideoPlaylistUseCase(expectedTitle)).thenReturn(expectedVideoPlaylist)
            whenever(videoPlaylistUIEntityMapper(anyOrNull())).thenReturn(
                expectedVideoPlaylistUIEntity
            )

            initUnderTest()

            underTest.createNewPlaylist(expectedTitle)
            underTest.state.drop(2).test {
                val actual = awaitItem()
                assertThat(actual.currentVideoPlaylist?.title).isEqualTo(expectedTitle)
                assertThat(actual.isVideoPlaylistCreatedSuccessfully).isTrue()
                cancelAndIgnoreRemainingEvents()
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
            val videoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn("playlist")
                on { id }.thenReturn(NodeId(1L))
            }
            val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { title }.thenReturn("playlist")
                on { id }.thenReturn(NodeId(0L))
            }

            whenever(addVideosToPlaylistUseCase(testPlaylistID, testVideoIDs)).thenReturn(
                testVideoIDs.size
            )
            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(videoPlaylist, videoPlaylist))
            whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)

            initUnderTest()
            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)
            underTest.addVideosToPlaylist(testPlaylistID, testVideoIDs)
            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.numberOfAddedVideos).isEqualTo(testVideoIDs.size)
                val updated = awaitItem()
                assertThat(updated.videoPlaylists).isNotEmpty()
                assertThat(updated.isPlaylistProgressBarShown).isFalse()
                assertThat(updated.currentVideoPlaylist?.title).isEqualTo(videoPlaylistUIEntity.title)
                assertThat(updated.currentVideoPlaylist?.id).isEqualTo(videoPlaylistUIEntity.id)
                underTest.clearNumberOfAddedVideos()
                assertThat(awaitItem().numberOfAddedVideos).isEqualTo(0)
            }
        }

    @Test
    fun `test that the number of removed videos is correct when removing videos from a playlist`() =
        runTest {
            val testPlaylistID = NodeId(1L)
            val testVideoElementIDs = listOf(1L, 2L, 3L)
            val videoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn("playlist")
                on { id }.thenReturn(NodeId(1L))
            }
            val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { title }.thenReturn("playlist")
                on { id }.thenReturn(NodeId(0L))
            }

            whenever(
                removeVideosFromPlaylistUseCase(
                    testPlaylistID,
                    testVideoElementIDs
                )
            ).thenReturn(
                testVideoElementIDs.size
            )
            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(videoPlaylist, videoPlaylist))
            whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)

            initUnderTest()
            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)
            underTest.removeVideosFromPlaylist(testPlaylistID, testVideoElementIDs)
            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.numberOfRemovedItems).isEqualTo(testVideoElementIDs.size)
                val updated = awaitItem()
                assertThat(updated.videoPlaylists).isNotEmpty()
                assertThat(updated.isPlaylistProgressBarShown).isFalse()
                assertThat(updated.currentVideoPlaylist?.title).isEqualTo(videoPlaylistUIEntity.title)
                assertThat(updated.currentVideoPlaylist?.id).isEqualTo(videoPlaylistUIEntity.id)
                underTest.clearNumberOfRemovedItems()
                assertThat(awaitItem().numberOfRemovedItems).isEqualTo(0)
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

    @Test
    fun `test that the currentVideoPlaylist is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().currentVideoPlaylist).isNull()
            underTest.updateCurrentVideoPlaylist(mock())
            assertThat(awaitItem().currentVideoPlaylist).isNotNull()
            underTest.updateCurrentVideoPlaylist(null)
            assertThat(awaitItem().currentVideoPlaylist).isNull()
        }
    }

    @Test
    fun `test that the showCreateVideoPlaylist is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().shouldCreateVideoPlaylist).isFalse()
            underTest.setShouldCreateVideoPlaylist(true)
            assertThat(awaitItem().shouldCreateVideoPlaylist).isTrue()
            underTest.setShouldCreateVideoPlaylist(false)
            assertThat(awaitItem().shouldCreateVideoPlaylist).isFalse()
        }
    }

    @Test
    fun `test that the shouldDeleteVideosFromPlaylist is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().shouldDeleteVideosFromPlaylist).isFalse()
            underTest.setShouldDeleteVideosFromPlaylist(true)
            assertThat(awaitItem().shouldDeleteVideosFromPlaylist).isTrue()
            underTest.setShouldDeleteVideosFromPlaylist(false)
            assertThat(awaitItem().shouldDeleteVideosFromPlaylist).isFalse()
        }
    }

    @Test
    fun `test that the actionMode is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().actionMode).isFalse()
            underTest.setActionMode(true)
            assertThat(awaitItem().actionMode).isTrue()
            underTest.setActionMode(false)
            assertThat(awaitItem().actionMode).isFalse()
        }
    }

    @Test
    fun `test that the createVideoPlaylistPlaceholderTitle is correctly updated`() = runTest {
        val expectedTitle = "new playlist"
        whenever(
            getNextDefaultAlbumNameUseCase(
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(expectedTitle)

        initUnderTest()

        underTest.setPlaceholderTitle(expectedTitle)

        underTest.state.test {
            assertThat(awaitItem().createVideoPlaylistPlaceholderTitle).isEqualTo(expectedTitle)
        }
    }

    @Test
    fun `test that the isInputTitleValid is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().isInputTitleValid).isTrue()
            underTest.setNewPlaylistTitleValidity(false)
            assertThat(awaitItem().isInputTitleValid).isFalse()
            underTest.setNewPlaylistTitleValidity(true)
            assertThat(awaitItem().isInputTitleValid).isTrue()
        }
    }

    @Test
    fun `test that the isVideoPlaylistCreatedSuccessfully is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().isVideoPlaylistCreatedSuccessfully).isFalse()
            underTest.setIsVideoPlaylistCreatedSuccessfully(true)
            assertThat(awaitItem().isVideoPlaylistCreatedSuccessfully).isTrue()
            underTest.setIsVideoPlaylistCreatedSuccessfully(false)
            assertThat(awaitItem().isVideoPlaylistCreatedSuccessfully).isFalse()
        }
    }

    @Test
    fun `test that the setShouldDeleteVideoPlaylist is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().shouldDeleteVideoPlaylist).isFalse()
            underTest.setShouldDeleteVideoPlaylist(true)
            assertThat(awaitItem().shouldDeleteVideoPlaylist).isTrue()
            underTest.setShouldDeleteVideoPlaylist(false)
            assertThat(awaitItem().shouldDeleteVideoPlaylist).isFalse()
        }
    }

    @Test
    fun `test that the setShouldDeleteSingleVideoPlaylist is correctly updated`() = runTest {
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().shouldDeleteSingleVideoPlaylist).isFalse()
            underTest.setShouldDeleteSingleVideoPlaylist(true)
            assertThat(awaitItem().shouldDeleteSingleVideoPlaylist).isTrue()
            underTest.setShouldDeleteSingleVideoPlaylist(false)
            assertThat(awaitItem().shouldDeleteSingleVideoPlaylist).isFalse()
        }
    }

    @Test
    fun `test that the setAreVideoPlaylistsRemovedSuccessfully is correctly updated`() =
        runTest {
            initUnderTest()

            underTest.state.test {
                assertThat(awaitItem().areVideoPlaylistsRemovedSuccessfully).isFalse()
                underTest.setAreVideoPlaylistsRemovedSuccessfully(true)
                assertThat(awaitItem().areVideoPlaylistsRemovedSuccessfully).isTrue()
                underTest.setAreVideoPlaylistsRemovedSuccessfully(false)
                assertThat(awaitItem().areVideoPlaylistsRemovedSuccessfully).isFalse()
            }
        }

    @Test
    fun `test that the setShouldShowMoreVideoPlaylistOptions is correctly updated`() =
        runTest {
            initUnderTest()

            underTest.state.test {
                assertThat(awaitItem().shouldShowMoreVideoPlaylistOptions).isFalse()
                underTest.setShouldShowMoreVideoPlaylistOptions(true)
                assertThat(awaitItem().shouldShowMoreVideoPlaylistOptions).isTrue()
                underTest.setShouldShowMoreVideoPlaylistOptions(false)
                assertThat(awaitItem().shouldShowMoreVideoPlaylistOptions).isFalse()
            }
        }

    @Test
    fun `test that the removeVideoPlaylists is correctly updated`() = runTest {
        val videoPlaylistTitles = listOf("new playlist", "new playlist1", "new playlist2")
        val playlistIDs = listOf(NodeId(1L), NodeId(2L), NodeId(3L))
        val deletedPlaylistIDs = listOf(NodeId(1L), NodeId(2L))
        val uiEntity1 = mock<VideoPlaylistUIEntity> {
            on { id }.thenReturn(playlistIDs[0])
            on { title }.thenReturn(videoPlaylistTitles[0])
        }
        val uiEntity2 = mock<VideoPlaylistUIEntity> {
            on { id }.thenReturn(playlistIDs[1])
            on { title }.thenReturn(videoPlaylistTitles[1])
        }

        whenever(removeVideoPlaylistsUseCase(deletedPlaylistIDs)).thenReturn(
            deletedPlaylistIDs.map { it.longValue }
        )

        underTest.removeVideoPlaylists(listOf(uiEntity1, uiEntity2))

        underTest.state.drop(1).test {
            val actual = awaitItem()
            assertThat(actual.deletedVideoPlaylistTitles.size).isEqualTo(2)
            assertThat(actual.deletedVideoPlaylistTitles[0]).isEqualTo(videoPlaylistTitles[0])
            assertThat(actual.areVideoPlaylistsRemovedSuccessfully).isTrue()
            assertThat(actual.shouldDeleteVideoPlaylist).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that shouldRenameVideoPlaylist is correctly updated after updated video playlist title`() =
        runTest {
            val newTitle = "newTitle"
            val playlistID = NodeId(1L)
            val videoPlaylist = mock<VideoPlaylist> {
                on { title }.thenReturn("playlist")
                on { id }.thenReturn(NodeId(1L))
            }
            val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { title }.thenReturn("playlist")
                on { id }.thenReturn(NodeId(0L))
            }
            val updatedVideoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { title }.thenReturn(newTitle)
                on { id }.thenReturn(NodeId(0L))
            }

            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(videoPlaylist, videoPlaylist))
            whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)
            whenever(updateVideoPlaylistTitleUseCase(playlistID, newTitle)).thenReturn(newTitle)
            whenever(videoPlaylistUIEntity.copy(title = newTitle)).thenReturn(
                updatedVideoPlaylistUIEntity
            )
            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)

            underTest.state.test {
                assertThat(awaitItem().shouldRenameVideoPlaylist).isFalse()
                underTest.setShouldRenameVideoPlaylist(true)
                assertThat(awaitItem().shouldRenameVideoPlaylist).isTrue()
                underTest.updateVideoPlaylistTitle(playlistID, newTitle)
                assertThat(awaitItem().shouldRenameVideoPlaylist).isFalse()

                val updated = awaitItem()
                assertThat(updated.videoPlaylists).isNotEmpty()
                assertThat(updated.isPlaylistProgressBarShown).isFalse()
                assertThat(updated.currentVideoPlaylist?.title).isEqualTo(newTitle)
                assertThat(updated.currentVideoPlaylist?.id).isEqualTo(videoPlaylistUIEntity.id)
            }
        }

    @Test
    fun `test that shouldRenameVideoPlaylist is correctly updated when throw exception`() =
        runTest {
            val newTitle = "newTitle"
            val playlistID = NodeId(1L)
            whenever(
                updateVideoPlaylistTitleUseCase(
                    playlistID,
                    newTitle
                )
            ).thenAnswer { throw Exception() }

            underTest.state.test {
                assertThat(awaitItem().shouldRenameVideoPlaylist).isFalse()
                underTest.setShouldRenameVideoPlaylist(true)
                assertThat(awaitItem().shouldRenameVideoPlaylist).isTrue()
                underTest.updateVideoPlaylistTitle(playlistID, newTitle)
                assertThat(awaitItem().shouldRenameVideoPlaylist).isFalse()
            }
        }

    @Test
    fun `test that the setCurrentDestinationRoute is correctly updated`() = runTest {
        val route = "route"
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().currentDestinationRoute).isNull()
            underTest.setCurrentDestinationRoute(route)
            assertThat(awaitItem().currentDestinationRoute).isEqualTo(route)
            underTest.setCurrentDestinationRoute(null)
            assertThat(awaitItem().currentDestinationRoute).isNull()
        }
    }

    @Test
    fun `test that the setLocationSelectedFilterOption is correctly updated`() = runTest {
        val locationOption = LocationFilterOption.CameraUploads
        val allLocations = LocationFilterOption.AllLocations
        initUnderTest()

        underTest.state.test {
            awaitItem().let {
                assertThat(it.locationSelectedFilterOption).isEqualTo(allLocations)
                assertThat(it.isPendingRefresh).isFalse()
            }
            underTest.setLocationSelectedFilterOption(locationOption)
            awaitItem().let {
                assertThat(it.locationSelectedFilterOption).isEqualTo(locationOption)
                assertThat(it.isPendingRefresh).isTrue()
                assertThat(it.progressBarShowing).isTrue()
            }
            underTest.setLocationSelectedFilterOption(allLocations)
            awaitItem().let {
                assertThat(it.locationSelectedFilterOption).isEqualTo(allLocations)
                assertThat(it.isPendingRefresh).isTrue()
            }
        }
    }

    @Test
    fun `test that the setDurationSelectedFilterOption is correctly updated`() = runTest {
        val durationOption = DurationFilterOption.MoreThan20
        val allDurations = DurationFilterOption.AllDurations
        initUnderTest()

        underTest.state.test {
            awaitItem().let {
                assertThat(it.durationSelectedFilterOption).isEqualTo(allDurations)
                assertThat(it.isPendingRefresh).isFalse()
            }
            underTest.setDurationSelectedFilterOption(durationOption)
            awaitItem().let {
                assertThat(it.durationSelectedFilterOption).isEqualTo(durationOption)
                assertThat(it.isPendingRefresh).isTrue()
                assertThat(it.progressBarShowing).isTrue()
            }
            underTest.setDurationSelectedFilterOption(allDurations)
            awaitItem().let {
                assertThat(it.durationSelectedFilterOption).isEqualTo(allDurations)
                assertThat(it.isPendingRefresh).isTrue()
            }
        }
    }

    @Test
    fun `test that the videos return correctly when the duration select option is not null`() =
        runTest {
            val durationOption = DurationFilterOption.MoreThan20

            val typedVideoNode1 = getTypedVideoNode(1)
            val typedVideoNode2 = getTypedVideoNode(2)
            val typedVideoNode3 = getTypedVideoNode(3)

            val videoOfDurationLessThan4 = getVideoUIEntityWithDuration(3.minutes)
            val videoOfDurationBetween4And20 = getVideoUIEntityWithDuration(8.minutes)
            val videoOfDurationMoreThan20 = getVideoUIEntityWithDuration(21.minutes)

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getAllVideosUseCase()).thenReturn(
                listOf(typedVideoNode1, typedVideoNode2, typedVideoNode3)
            )
            whenever(videoUIEntityMapper(typedVideoNode1)).thenReturn(videoOfDurationLessThan4)
            whenever(videoUIEntityMapper(typedVideoNode2)).thenReturn(videoOfDurationBetween4And20)
            whenever(videoUIEntityMapper(typedVideoNode3)).thenReturn(videoOfDurationMoreThan20)

            underTest.setDurationSelectedFilterOption(durationOption)
            underTest.refreshNodes()

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.durationSelectedFilterOption).isEqualTo(durationOption)
                assertThat(actual.allVideos.size).isEqualTo(1)
                assertThat(actual.allVideos[0].duration)
                    .isEqualTo(videoOfDurationMoreThan20.duration)
            }
        }

    @Test
    fun `test that the videos return correctly when the duration select option is AllDuration`() =
        runTest {
            val durationOption = DurationFilterOption.AllDurations

            val typedVideoNode1 = getTypedVideoNode(1)
            val typedVideoNode2 = getTypedVideoNode(2)
            val typedVideoNode3 = getTypedVideoNode(3)

            val videoOfDurationLessThan4 = getVideoUIEntityWithDuration(3.minutes)
            val videoOfDurationBetween4And20 = getVideoUIEntityWithDuration(8.minutes)
            val videoOfDurationMoreThan20 = getVideoUIEntityWithDuration(21.minutes)

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getAllVideosUseCase()).thenReturn(
                listOf(typedVideoNode1, typedVideoNode2, typedVideoNode3)
            )
            whenever(videoUIEntityMapper(typedVideoNode1)).thenReturn(videoOfDurationLessThan4)
            whenever(videoUIEntityMapper(typedVideoNode2)).thenReturn(videoOfDurationBetween4And20)
            whenever(videoUIEntityMapper(typedVideoNode3)).thenReturn(videoOfDurationMoreThan20)

            underTest.setDurationSelectedFilterOption(durationOption)
            underTest.refreshNodes()

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.durationSelectedFilterOption).isEqualTo(durationOption)
                assertThat(actual.allVideos.size).isEqualTo(3)
            }
        }

    private fun getTypedVideoNode(videoId: Long) = mock<TypedVideoNode> {
        on { id }.thenReturn(NodeId(videoId))
        on { name }.thenReturn("video name")
    }

    private fun getVideoUIEntityWithDuration(value: Duration) = mock<VideoUIEntity> {
        on { name }.thenReturn("video name")
        on { duration }.thenReturn(value)
    }

    @Test
    fun `test that the videos return correctly when the location select option is CameraUploads`() =
        runTest {
            val video1 = getVideoUIEntityWithParentIdAndShared(5)
            val video2 = getVideoUIEntityWithParentIdAndShared(7)
            val video3 = getVideoUIEntityWithParentIdAndShared(4)

            initFilterOptionTestData(
                LocationFilterOption.CameraUploads,
                listOf(video1, video2, video3)
            )

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.locationSelectedFilterOption).isEqualTo(
                    LocationFilterOption.CameraUploads
                )
                assertThat(actual.allVideos.size).isEqualTo(1)
                assertThat(actual.allVideos[0].parentId).isEqualTo(NodeId(7))
            }
        }

    @Test
    fun `test that the videos return correctly when the location select option is SharedItems`() =
        runTest {
            val video1 = getVideoUIEntityWithParentIdAndShared(5, true)
            val video2 = getVideoUIEntityWithParentIdAndShared(7, false)
            val video3 = getVideoUIEntityWithParentIdAndShared(4, false)

            initFilterOptionTestData(
                LocationFilterOption.SharedItems,
                listOf(video1, video2, video3)
            )

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.locationSelectedFilterOption).isEqualTo(
                    LocationFilterOption.SharedItems
                )
                assertThat(actual.allVideos.size).isEqualTo(1)
                assertThat(actual.allVideos[0].parentId.longValue).isEqualTo(5)
            }
        }

    @Test
    fun `test that the videos return correctly when the location select option is CloudDrive`() =
        runTest {
            val video1 = getVideoUIEntityWithParentIdAndShared(5)
            val video2 = getVideoUIEntityWithParentIdAndShared(7)
            val video3 = getVideoUIEntityWithParentIdAndShared(4)

            initFilterOptionTestData(
                LocationFilterOption.CloudDrive,
                listOf(video1, video2, video3)
            )

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.locationSelectedFilterOption).isEqualTo(
                    LocationFilterOption.CloudDrive
                )
                assertThat(actual.allVideos.size).isEqualTo(2)
            }
        }

    @Test
    fun `test that the videos return correctly when the location select option is AllLocations`() =
        runTest {
            val video1 = getVideoUIEntityWithParentIdAndShared(5)
            val video2 = getVideoUIEntityWithParentIdAndShared(7)
            val video3 = getVideoUIEntityWithParentIdAndShared(4)

            initFilterOptionTestData(
                LocationFilterOption.AllLocations,
                listOf(video1, video2, video3)
            )

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.locationSelectedFilterOption).isEqualTo(
                    LocationFilterOption.AllLocations
                )
                assertThat(actual.allVideos.size).isEqualTo(3)
            }
        }

    private suspend fun initFilterOptionTestData(
        locationFilterOption: LocationFilterOption,
        videos: List<VideoUIEntity>,
    ) {
        val typedVideoNode1 = getTypedVideoNode(1)
        val typedVideoNode2 = getTypedVideoNode(2)
        val typedVideoNode3 = getTypedVideoNode(3)

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllVideosUseCase()).thenReturn(
            listOf(typedVideoNode1, typedVideoNode2, typedVideoNode3)
        )
        whenever(getSyncUploadsFolderIdsUseCase()).thenReturn(listOf(7))
        whenever(videoUIEntityMapper(typedVideoNode1)).thenReturn(videos[0])
        whenever(videoUIEntityMapper(typedVideoNode2)).thenReturn(videos[1])
        whenever(videoUIEntityMapper(typedVideoNode3)).thenReturn(videos[2])

        underTest.setLocationSelectedFilterOption(locationFilterOption)
        underTest.refreshNodes()
    }

    private fun getVideoUIEntityWithParentIdAndShared(pId: Long, shared: Boolean = false) =
        mock<VideoUIEntity> {
            on { name }.thenReturn("video name")
            on { parentId }.thenReturn(NodeId(pId))
            on { isSharedItems }.thenReturn(shared)
        }

    @Test
    fun `test that the updateToolbarTitle is updated correctly`() = runTest {
        val expectedTitle = "title"
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().updateToolbarTitle).isNull()
            underTest.setUpdateToolbarTitle(expectedTitle)
            assertThat(awaitItem().updateToolbarTitle).isEqualTo(expectedTitle)
            underTest.setUpdateToolbarTitle(null)
            assertThat(awaitItem().updateToolbarTitle).isNull()
        }
    }

    @Test
    fun `test that state is updated correctly when monitorVideoPlaylistSetsUpdateUseCase is triggered`() =
        runTest {
            initVideoPlaylistsReturned()
            initUnderTest()
            testScheduler.advanceUntilIdle()

            underTest.state.drop(1).test {
                fakeMonitorVideoPlaylistSetsUpdateFlow.emit(listOf(1L, 2L, 3L))
                val actual = awaitItem()
                assertThat(actual.videoPlaylists.size).isEqualTo(2)
                assertThat(actual.isPlaylistProgressBarShown).isFalse()
            }
        }

    @Test
    fun `test that getTypedVideoNodeById function returns the correct node`() = runTest {
        val typedNodes = (0..3).map { handle ->
            mock<TypedVideoNode> { on { id }.thenReturn(NodeId(handle.toLong())) }
        }
        initVideosReturned()
        whenever(getAllVideosUseCase()).thenReturn(typedNodes)

        underTest.refreshNodes()
        delay(100)
        val result = underTest.getTypedVideoNodeById(typedNodes[1].id)
        assertThat(result).isEqualTo(typedNodes[1])
    }

    @Test
    fun `test that getTypedVideoNodeOfPlaylistById function returns the correct node`() = runTest {
        val expectedVideos = (0..3).map { handle ->
            mock<TypedVideoNode> { on { id }.thenReturn(NodeId(handle.toLong())) }
        }
        val videoPlaylist = mock<VideoPlaylist> {
            on { id }.thenReturn(NodeId(1L))
            on { videos }.thenReturn(expectedVideos)
        }
        val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
            on { id }.thenReturn(NodeId(1L))
            on { title }.thenReturn("title")
        }
        whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(videoPlaylist))
        whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)

        initUnderTest()
        underTest.onTabSelected(VideoSectionTab.Playlists)
        underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)
        delay(100)
        val result = underTest.getTypedVideoNodeOfPlaylistById(expectedVideos[1].id)
        assertThat(result).isEqualTo(expectedVideos[1])
    }

    @Test
    fun `test that getNodeContentUri function returns the correct uri`() = runTest {
        val url = "url"
        val uri = NodeContentUri.RemoteContentUri(url, true)
        initVideosReturned()
        whenever(getNodeContentUriUseCase(anyOrNull())).thenReturn(uri)

        val result = underTest.getNodeContentUri(mock())
        assertThat(result).isEqualTo(uri)
    }

    @Test
    fun `test that getNodeContentUri function returns null when the exception is thrown`() =
        runTest {
            initVideosReturned()
            whenever(getNodeContentUriUseCase(anyOrNull())).thenThrow(NullPointerException())

            val result = underTest.getNodeContentUri(mock())
            assertThat(result).isNull()
        }

    @Test
    fun `test that clickedItem is updated correctly`() = runTest {
        val expectedVideoNode = mock<TypedVideoNode>()
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().clickedItem).isNull()
            underTest.updateClickedItem(expectedVideoNode)
            assertThat(awaitItem().clickedItem).isEqualTo(expectedVideoNode)
            underTest.updateClickedItem(null)
            assertThat(awaitItem().clickedItem).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clickedPlaylistDetailItem is updated correctly`() = runTest {
        val expectedVideoNode = mock<TypedVideoNode>()
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().clickedPlaylistDetailItem).isNull()
            underTest.updateClickedPlaylistDetailItem(expectedVideoNode)
            assertThat(awaitItem().clickedPlaylistDetailItem).isEqualTo(expectedVideoNode)
            underTest.updateClickedPlaylistDetailItem(null)
            assertThat(awaitItem().clickedPlaylistDetailItem).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clickedItem is updated correctly when onItemClicked is invoked`() =
        runTest {
            val expectedVideoNode = mock<TypedVideoNode> {
                on { id }.thenReturn(expectedId)
            }
            initVideosReturned()
            whenever(getAllVideosUseCase()).thenReturn(listOf(mock(), expectedVideoNode))
            initUnderTest()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos.size).isEqualTo(2)

                underTest.onItemClicked(expectedVideo, 1)
                assertThat(awaitItem().clickedItem).isEqualTo(expectedVideoNode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that clickedPlaylistDetailItem is updated correctly when onVideoItemOfPlaylistClicked is invoked`() =
        runTest {
            val expectedId = NodeId(1L)
            val expectedVideoNode = mock<TypedVideoNode> {
                on { id }.thenReturn(expectedId)
            }
            val videoPlaylist = mock<VideoPlaylist> {
                on { id }.thenReturn(expectedId)
                on { videos }.thenReturn(listOf(expectedVideoNode))
            }
            val videoUIEntity = mock<VideoUIEntity> {
                on { id }.thenReturn(expectedId)
            }
            val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { id }.thenReturn(expectedId)
                on { title }.thenReturn("title")
                on { videos }.thenReturn(listOf(videoUIEntity))
            }

            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(videoPlaylist))
            whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)

            initUnderTest()
            underTest.onTabSelected(VideoSectionTab.Playlists)
            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)

            underTest.state.drop(1).test {
                assertThat(awaitItem().videoPlaylists).isNotEmpty()
                underTest.onVideoItemOfPlaylistClicked(videoUIEntity, 0)
                assertThat(awaitItem().clickedPlaylistDetailItem).isEqualTo(expectedVideoNode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that clickedPlaylistDetailItem is updated correctly when playAllButtonClicked is invoked`() =
        runTest {
            val expectedId = NodeId(1L)
            val expectedVideoNode = mock<TypedVideoNode> {
                on { id }.thenReturn(expectedId)
            }
            val videoPlaylist = mock<VideoPlaylist> {
                on { id }.thenReturn(expectedId)
                on { videos }.thenReturn(listOf(expectedVideoNode))
            }
            val videoUIEntity = mock<VideoUIEntity> {
                on { id }.thenReturn(expectedId)
            }
            val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
                on { id }.thenReturn(expectedId)
                on { title }.thenReturn("title")
                on { videos }.thenReturn(listOf(videoUIEntity))
            }

            whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(videoPlaylist))
            whenever(videoPlaylistUIEntityMapper(videoPlaylist)).thenReturn(videoPlaylistUIEntity)

            initUnderTest()
            underTest.onTabSelected(VideoSectionTab.Playlists)
            underTest.updateCurrentVideoPlaylist(videoPlaylistUIEntity)

            underTest.state.drop(1).test {
                assertThat(awaitItem().videoPlaylists).isNotEmpty()
                underTest.playAllButtonClicked()
                assertThat(awaitItem().clickedPlaylistDetailItem).isEqualTo(expectedVideoNode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
