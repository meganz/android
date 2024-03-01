package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
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
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getVideoPlaylistsUseCase = mock<GetVideoPlaylistsUseCase>()
    private val videoPlaylistUIEntityMapper = mock<VideoPlaylistUIEntityMapper>()
    private val createVideoPlaylistUseCase = mock<CreateVideoPlaylistUseCase>()
    private val addVideosToPlaylistUseCase = mock<AddVideosToPlaylistUseCase>()
    private val getNextDefaultAlbumNameUseCase = mock<GetNextDefaultAlbumNameUseCase>()
    private val removeVideoPlaylistsUseCase = mock<RemoveVideoPlaylistsUseCase>()
    private val updateVideoPlaylistTitleUseCase = mock<UpdateVideoPlaylistTitleUseCase>()
    private val getSyncUploadsFolderIdsUseCase = mock<GetSyncUploadsFolderIdsUseCase>()

    private val expectedVideo = mock<VideoUIEntity> { on { name }.thenReturn("video name") }

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
            videoUIEntityMapper = videoUIEntityMapper,
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
            videoPlaylistUIEntityMapper = videoPlaylistUIEntityMapper,
            createVideoPlaylistUseCase = createVideoPlaylistUseCase,
            addVideosToPlaylistUseCase = addVideosToPlaylistUseCase,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
            removeVideoPlaylistsUseCase = removeVideoPlaylistsUseCase,
            updateVideoPlaylistTitleUseCase = updateVideoPlaylistTitleUseCase,
            getSyncUploadsFolderIdsUseCase = getSyncUploadsFolderIdsUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            videoUIEntityMapper,
            getCloudSortOrder,
            getNodeByHandle,
            getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase,
            getNodeByIdUseCase,
            getVideoPlaylistsUseCase,
            videoPlaylistUIEntityMapper,
            createVideoPlaylistUseCase,
            addVideosToPlaylistUseCase,
            getNextDefaultAlbumNameUseCase,
            updateVideoPlaylistTitleUseCase,
            getSyncUploadsFolderIdsUseCase
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
        val videoPlaylistUIEntity = mock<VideoPlaylistUIEntity> {
            on { title }.thenReturn("playlist")
        }
        whenever(getVideoPlaylistsUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(videoPlaylistUIEntityMapper(any())).thenReturn(videoPlaylistUIEntity)
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
            underTest.state.drop(1).test {
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

            whenever(updateVideoPlaylistTitleUseCase(playlistID, newTitle)).thenReturn(
                newTitle
            )

            underTest.state.test {
                assertThat(awaitItem().shouldRenameVideoPlaylist).isFalse()
                underTest.setShouldRenameVideoPlaylist(true)
                assertThat(awaitItem().shouldRenameVideoPlaylist).isTrue()
                underTest.updateVideoPlaylistTitle(playlistID, newTitle)
                assertThat(awaitItem().shouldRenameVideoPlaylist).isFalse()
                cancelAndIgnoreRemainingEvents()
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
        initUnderTest()

        underTest.state.test {
            awaitItem().let {
                assertThat(it.locationSelectedFilterOption).isNull()
                assertThat(it.isPendingRefresh).isFalse()
            }
            underTest.setLocationSelectedFilterOption(locationOption)
            awaitItem().let {
                assertThat(it.locationSelectedFilterOption).isEqualTo(locationOption)
                assertThat(it.isPendingRefresh).isTrue()
                assertThat(it.progressBarShowing).isTrue()
            }
            underTest.setLocationSelectedFilterOption(null)
            awaitItem().let {
                assertThat(it.locationSelectedFilterOption).isNull()
                assertThat(it.isPendingRefresh).isTrue()
            }
        }
    }

    @Test
    fun `test that the setDurationSelectedFilterOption is correctly updated`() = runTest {
        val durationOption = DurationFilterOption.MoreThan20
        initUnderTest()

        underTest.state.test {
            awaitItem().let {
                assertThat(it.durationSelectedFilterOption).isNull()
                assertThat(it.isPendingRefresh).isFalse()
            }
            underTest.setDurationSelectedFilterOption(durationOption)
            awaitItem().let {
                assertThat(it.durationSelectedFilterOption).isEqualTo(durationOption)
                assertThat(it.isPendingRefresh).isTrue()
                assertThat(it.progressBarShowing).isTrue()
            }
            underTest.setDurationSelectedFilterOption(null)
            awaitItem().let {
                assertThat(it.durationSelectedFilterOption).isNull()
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

            val videoOfDurationLessThan4 = getVideoUIEntityWithDuration(3)
            val videoOfDurationBetween4And20 = getVideoUIEntityWithDuration(8)
            val videoOfDurationMoreThan20 = getVideoUIEntityWithDuration(21)

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
                assertThat(actual.allVideos[0].durationInMinutes)
                    .isEqualTo(videoOfDurationMoreThan20.durationInMinutes)
            }
        }

    private fun getTypedVideoNode(videoId: Long) = mock<TypedVideoNode> {
        on { id }.thenReturn(NodeId(videoId))
        on { name }.thenReturn("video name")
    }

    private fun getVideoUIEntityWithDuration(minutes: Long) = mock<VideoUIEntity> {
        on { name }.thenReturn("video name")
        on { durationInMinutes }.thenReturn(minutes)
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

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
