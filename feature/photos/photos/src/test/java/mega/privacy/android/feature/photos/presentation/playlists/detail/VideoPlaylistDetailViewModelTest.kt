package mega.privacy.android.feature.photos.presentation.playlists.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistByIdUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideosFromPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistDetailUiEntityMapper
import mega.privacy.android.feature.photos.mapper.VideoPlaylistTitleValidationErrorMessageMapper
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
    private val videoPlaylistTitleValidationErrorMessageMapper =
        mock<VideoPlaylistTitleValidationErrorMessageMapper>()
    private val updateVideoPlaylistTitleUseCase = mock<UpdateVideoPlaylistTitleUseCase>()
    private val removeVideoPlaylistsUseCase = mock<RemoveVideoPlaylistsUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val getNodeContentUriByHandleUseCase = mock<GetNodeContentUriByHandleUseCase>()
    private val removeVideosFromPlaylistUseCase = mock<RemoveVideosFromPlaylistUseCase>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val nodeSortConfigurationUiMapper = NodeSortConfigurationUiMapper()
    private val nodeSourceTypeToViewTypeMapper = NodeSourceTypeToViewTypeMapper()

    private val sortOrderFlow = MutableStateFlow<SortOrder?>(SortOrder.ORDER_DEFAULT_ASC)

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
        val args = VideoPlaylistDetailViewModel.Args(
            playlistHandle = testId.longValue,
            type = testType,
        )
        underTest = VideoPlaylistDetailViewModel(
            videoPlaylistDetailUiEntityMapper = videoPlaylistDetailUiEntityMapper,
            getVideoPlaylistByIdUseCase = getVideoPlaylistByIdUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase = monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistTitleValidationErrorMessageMapper = videoPlaylistTitleValidationErrorMessageMapper,
            updateVideoPlaylistTitleUseCase = updateVideoPlaylistTitleUseCase,
            removeVideoPlaylistsUseCase = removeVideoPlaylistsUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            nodeSourceTypeToViewTypeMapper = nodeSourceTypeToViewTypeMapper,
            getNodeContentUriByHandleUseCase = getNodeContentUriByHandleUseCase,
            removeVideosFromPlaylistUseCase = removeVideosFromPlaylistUseCase,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            args = args,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistDetailUiEntityMapper,
            getVideoPlaylistByIdUseCase,
            videoPlaylistTitleValidationErrorMessageMapper,
            updateVideoPlaylistTitleUseCase,
            removeVideoPlaylistsUseCase,
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
            getNodeContentUriByHandleUseCase,
            removeVideosFromPlaylistUseCase,
            monitorSortCloudOrderUseCase,
            setCloudSortOrderUseCase
        )
        sortOrderFlow.value = SortOrder.ORDER_DEFAULT_ASC
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
                assertThat(actual.selectedSortConfiguration).isEqualTo(
                    nodeSortConfigurationUiMapper(
                        sortOrderFlow.value ?: SortOrder.ORDER_DEFAULT_ASC
                    )
                )
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

    @Test
    fun `test that getVideoPlaylistByIdUseCase is invoked when monitorSortCloudOrderUseCase is triggered`() =
        runTest {
            stubInitialValues()
            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.playlistDetail).isNotNull()
                    clearInvocations(getVideoPlaylistByIdUseCase)

                    sortOrderFlow.emit(SortOrder.ORDER_MODIFICATION_DESC)
                    awaitItem()
                    verify(getVideoPlaylistByIdUseCase).invoke(any(), any())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedSortConfiguration is derived from monitorSortCloudOrderUseCase`() =
        runTest {
            stubInitialValues()
            val expectedConfig = NodeSortConfiguration(
                NodeSortOption.Modified,
                SortDirection.Descending
            )
            sortOrderFlow.value = SortOrder.ORDER_MODIFICATION_DESC

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    val actual = awaitItem()
                    assertThat(actual.selectedSortConfiguration).isEqualTo(expectedConfig)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that setCloudSortOrder invokes setCloudSortOrderUseCase with mapped order`() =
        runTest {
            stubInitialValues()
            val sortConfiguration = NodeSortConfiguration(
                NodeSortOption.Name,
                SortDirection.Ascending
            )
            val expectedOrder = nodeSortConfigurationUiMapper(sortConfiguration)

            underTest.setCloudSortOrder(sortConfiguration)
            advanceUntilIdle()

            verify(setCloudSortOrderUseCase).invoke(expectedOrder)
        }

    @Test
    fun `test that setCloudSortOrder does not throw when setCloudSortOrderUseCase fails`() =
        runTest {
            stubInitialValues()
            whenever(setCloudSortOrderUseCase(any())).thenThrow(RuntimeException("test failure"))

            underTest.setCloudSortOrder(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
            )
            advanceUntilIdle()

            verify(setCloudSortOrderUseCase).invoke(any())
        }

    private suspend fun stubInitialValues(
        videoPlaylist: VideoPlaylist = expectedVideoPlaylist,
        detailEntity: VideoPlaylistDetailUiEntity = expectedPlaylistDetail,
        showHiddenItems: Boolean = false,
        isHiddenNodesEnabled: Boolean = true,
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
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(
            flow {
                emit(isHiddenNodesEnabled)
                awaitCancellation()
            }
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(
            flow {
                emit(showHiddenItems)
                awaitCancellation()
            }
        )
        whenever(getVideoPlaylistByIdUseCase(any(), any())).thenReturn(videoPlaylist)
        whenever(
            videoPlaylistDetailUiEntityMapper(
                videoPlaylist = any(),
                showHiddenItems = any(),
                selectedIds = any()
            )
        ).thenReturn(detailEntity)
        whenever(monitorSortCloudOrderUseCase()).thenReturn(sortOrderFlow)
    }

    @Test
    fun `test that updateVideoPlaylistTitle updates title successfully`() = runTest {
        val playlistId = NodeId(1L)
        val newTitle = "New Title"
        val trimmedTitle = newTitle.trim()
        whenever(updateVideoPlaylistTitleUseCase(playlistId, trimmedTitle)).thenReturn(trimmedTitle)

        underTest.updateVideoPlaylistTitle(playlistId, newTitle)

        underTest.videoPlaylistEditState.test {
            verify(updateVideoPlaylistTitleUseCase).invoke(playlistId, trimmedTitle)
            val actual = awaitItem()
            assertThat(actual.editVideoPlaylistErrorMessage).isNull()
            assertThat(actual.updateTitleSuccessEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that resetUpdateTitleSuccessEvent reset the updateTitleSuccessEvent`() = runTest {
        val playlistId = NodeId(1L)
        val newTitle = "New Title"
        val trimmedTitle = newTitle.trim()
        whenever(updateVideoPlaylistTitleUseCase(playlistId, trimmedTitle)).thenReturn(trimmedTitle)

        underTest.updateVideoPlaylistTitle(playlistId, newTitle)

        underTest.videoPlaylistEditState.test {
            assertThat(awaitItem().updateTitleSuccessEvent).isEqualTo(triggered)

            underTest.resetUpdateTitleSuccessEvent()
            assertThat(awaitItem().updateTitleSuccessEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that updateVideoPlaylistTitle sets error message when Empty exception is thrown`() =
        runTest {
            val playlistId = NodeId(1L)
            val newTitle = "   "
            val errorMessage = "Error message"
            whenever(updateVideoPlaylistTitleUseCase(any(), any())).thenAnswer {
                throw PlaylistNameValidationException.Empty
            }
            whenever(
                videoPlaylistTitleValidationErrorMessageMapper(
                    PlaylistNameValidationException.Empty
                )
            ).thenReturn(errorMessage)

            underTest.updateVideoPlaylistTitle(playlistId, newTitle)

            underTest.videoPlaylistEditState.test {
                assertThat(underTest.videoPlaylistEditState.value.editVideoPlaylistErrorMessage)
                    .isEqualTo(errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updateVideoPlaylistTitle sets error message when Exists exception is thrown`() =
        runTest {
            val playlistId = NodeId(1L)
            val newTitle = "Existing Title"
            val errorMessage = "Error message"
            whenever(updateVideoPlaylistTitleUseCase(any(), any())).thenAnswer {
                throw PlaylistNameValidationException.Exists
            }
            whenever(
                videoPlaylistTitleValidationErrorMessageMapper(
                    PlaylistNameValidationException.Exists
                )
            ).thenReturn(errorMessage)

            underTest.updateVideoPlaylistTitle(playlistId, newTitle)

            underTest.videoPlaylistEditState.test {
                assertThat(underTest.videoPlaylistEditState.value.editVideoPlaylistErrorMessage)
                    .isEqualTo(errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updateVideoPlaylistTitle sets error message when InvalidCharacters exception is thrown`() =
        runTest {
            val playlistId = NodeId(1L)
            val newTitle = "Invalid<Title>"
            val errorMessage = "Error message"
            val exception = PlaylistNameValidationException.InvalidCharacters("<>")
            whenever(updateVideoPlaylistTitleUseCase(any(), any())).thenAnswer {
                throw exception
            }
            whenever(
                videoPlaylistTitleValidationErrorMessageMapper(exception)
            ).thenReturn(errorMessage)

            underTest.updateVideoPlaylistTitle(playlistId, newTitle)

            underTest.videoPlaylistEditState.test {
                assertThat(underTest.videoPlaylistEditState.value.editVideoPlaylistErrorMessage)
                    .isEqualTo(errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetEditVideoPlaylistErrorMessage clears error message`() = runTest {
        val playlistId = NodeId(1L)
        val newTitle = "   "
        val errorMessage = "Error message"
        whenever(updateVideoPlaylistTitleUseCase(any(), any())).thenAnswer {
            throw PlaylistNameValidationException.Empty
        }
        whenever(
            videoPlaylistTitleValidationErrorMessageMapper(
                PlaylistNameValidationException.Empty
            )
        ).thenReturn(errorMessage)

        underTest.updateVideoPlaylistTitle(playlistId, newTitle)

        underTest.videoPlaylistEditState.test {
            assertThat(underTest.videoPlaylistEditState.value.editVideoPlaylistErrorMessage)
                .isEqualTo(errorMessage)

            underTest.resetEditVideoPlaylistErrorMessage()
            assertThat(underTest.videoPlaylistEditState.value.editVideoPlaylistErrorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetUpdateVideoPlaylistDialogEvent updated correctly`() = runTest {
        underTest.videoPlaylistEditState.test {
            assertThat(awaitItem().showUpdateVideoPlaylist).isFalse()

            underTest.showUpdateVideoPlaylistDialog()
            assertThat(awaitItem().showUpdateVideoPlaylist).isTrue()

            underTest.resetUpdateVideoPlaylistDialogEvent()
            assertThat(awaitItem().showUpdateVideoPlaylist).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that playlistsRemovedEvent is updated correctly`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            whenever(
                removeVideoPlaylistsUseCase(any())
            ).thenReturn(listOf(playlist1.id.longValue, playlist2.id.longValue))

            underTest.removeVideoPlaylists(setOf(playlist1, playlist2))
            advanceUntilIdle()

            underTest.videoPlaylistEditState.test {
                val expectedEvent = triggered(listOf(playlist1.title, playlist2.title))
                assertThat(awaitItem().playlistsRemovedEvent).isEqualTo(expectedEvent)

                underTest.resetPlaylistsRemovedEvent()
                assertThat(awaitItem().playlistsRemovedEvent).isEqualTo(consumed())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly when onItemLongClicked`() = runTest {
        val video1 = createVideoUiEntity(handle = 1L)
        val video2 = createVideoUiEntity(handle = 2L)
        val typedNode1 = createTypedVideoNode(video1)
        val typedNode2 = createTypedVideoNode(video2)
        val playlistWithVideos = createVideoPlaylist(
            expectedPlaylistUiEntity,
            videos = listOf(typedNode1, typedNode2)
        )
        val detailWithVideos = mock<VideoPlaylistDetailUiEntity> {
            on { uiEntity }.thenReturn(expectedPlaylistUiEntity)
            on { videos }.thenReturn(listOf(video1, video2))
        }
        stubInitialValues(
            videoPlaylist = playlistWithVideos,
            detailEntity = detailWithVideos
        )

        underTest.uiState
            .filterIsInstance<VideoPlaylistDetailUiState.Data>()
            .test {
                var actual = awaitItem()
                assertThat(actual.selectedTypedNodes).isEmpty()

                underTest.onItemLongClicked(video1)
                actual = awaitItem()
                assertThat(actual.selectedTypedNodes).hasSize(1)
                assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(video1.id)

                underTest.onItemLongClicked(video2)
                actual = awaitItem()
                assertThat(actual.selectedTypedNodes).hasSize(2)
                assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(
                    video1.id,
                    video2.id
                )
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that selectedTypedNodes are updated when onItemClicked and selection is not empty`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)
            val typedNode1 = createTypedVideoNode(video1)
            val typedNode2 = createTypedVideoNode(video2)
            val playlistWithVideos = createVideoPlaylist(
                expectedPlaylistUiEntity,
                videos = listOf(typedNode1, typedNode2)
            )
            val detailWithVideos = mock<VideoPlaylistDetailUiEntity> {
                on { uiEntity }.thenReturn(expectedPlaylistUiEntity)
                on { videos }.thenReturn(listOf(video1, video2))
            }
            stubInitialValues(
                videoPlaylist = playlistWithVideos,
                detailEntity = detailWithVideos
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemLongClicked(video1)
                    var actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(1)

                    underTest.onItemClicked(video2)
                    actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(2)
                    assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(
                        video1.id,
                        video2.id
                    )
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after selectAllVideos is invoked`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)
            val typedNode1 = createTypedVideoNode(video1)
            val typedNode2 = createTypedVideoNode(video2)
            val playlistWithVideos = createVideoPlaylist(
                expectedPlaylistUiEntity,
                videos = listOf(typedNode1, typedNode2)
            )
            val detailWithVideos = mock<VideoPlaylistDetailUiEntity> {
                on { uiEntity }.thenReturn(expectedPlaylistUiEntity)
                on { videos }.thenReturn(listOf(video1, video2))
            }
            stubInitialValues(
                videoPlaylist = playlistWithVideos,
                detailEntity = detailWithVideos
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    var actual = awaitItem()
                    assertThat(actual.playlistDetail?.videos).isNotEmpty()
                    assertThat(actual.selectedTypedNodes).isEmpty()

                    underTest.selectAllVideos()
                    actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(2)
                    assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(
                        video1.id,
                        video2.id
                    )
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after clearSelection is invoked`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)
            val typedNode1 = createTypedVideoNode(video1)
            val typedNode2 = createTypedVideoNode(video2)
            val playlistWithVideos = createVideoPlaylist(
                expectedPlaylistUiEntity,
                videos = listOf(typedNode1, typedNode2)
            )
            val detailWithVideos = mock<VideoPlaylistDetailUiEntity> {
                on { uiEntity }.thenReturn(expectedPlaylistUiEntity)
                on { videos }.thenReturn(listOf(video1, video2))
            }
            stubInitialValues(
                videoPlaylist = playlistWithVideos,
                detailEntity = detailWithVideos
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemLongClicked(video1)
                    var actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(1)
                    assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(video1.id)

                    underTest.clearSelection()
                    actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).isEmpty()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedElementIds is derived from playlistDetail videos and selectedTypedNodes`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L, elementID = 10L)
            val video2 = createVideoUiEntity(handle = 2L, elementID = 20L)
            val typedNode1 = createTypedVideoNode(video1)
            val typedNode2 = createTypedVideoNode(video2)
            val playlistWithVideos = createVideoPlaylist(
                expectedPlaylistUiEntity,
                videos = listOf(typedNode1, typedNode2)
            )
            val detailWithVideos = mock<VideoPlaylistDetailUiEntity> {
                on { uiEntity }.thenReturn(expectedPlaylistUiEntity)
                on { videos }.thenReturn(listOf(video1, video2))
            }
            stubInitialValues(
                videoPlaylist = playlistWithVideos,
                detailEntity = detailWithVideos
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    var actual = awaitItem()
                    assertThat(actual.selectedElementIds).isEmpty()

                    underTest.onItemLongClicked(video1)
                    actual = awaitItem()
                    assertThat(actual.selectedElementIds).containsExactly(10L)

                    underTest.onItemLongClicked(video2)
                    actual = awaitItem()
                    assertThat(actual.selectedElementIds).containsExactlyElementsIn(setOf(10L, 20L))

                    underTest.clearSelection()
                    actual = awaitItem()
                    assertThat(actual.selectedElementIds).isEmpty()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @ParameterizedTest(name = "when showHiddenItems is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that uiState showHiddenItems is updated correctly`(showHiddenItems: Boolean) =
        runTest {
            stubInitialValues(showHiddenItems = showHiddenItems)

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    val actual = awaitItem()
                    assertThat(actual.showHiddenItems).isEqualTo(showHiddenItems)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @ParameterizedTest(name = "when isHiddenNodesEnabled is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that uiState isHiddenNodesEnabled is updated correctly`(
        isHiddenNodesEnabled: Boolean,
    ) = runTest {
        stubInitialValues(isHiddenNodesEnabled = isHiddenNodesEnabled)

        underTest.uiState
            .filterIsInstance<VideoPlaylistDetailUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.isHiddenNodesEnabled).isEqualTo(isHiddenNodesEnabled)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that onItemClicked when selection is empty triggers navigateToVideoPlayer with correct NavKey`() =
        runTest {
            val video = createVideoUiEntity(handle = 1L, name = "test-video")
            val contentUri = NodeContentUri.RemoteContentUri("http://test.url", false)
            stubInitialValues()
            whenever(getNodeContentUriByHandleUseCase(video.id.longValue)).thenReturn(contentUri)

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemClicked(video)
                    advanceUntilIdle()

                    val event = underTest.navigateToVideoPlayerEvent.value
                    assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                    val navKey =
                        (event as StateEventWithContentTriggered).content as LegacyMediaPlayerNavKey
                    assertThat(navKey.nodeHandle).isEqualTo(video.id.longValue)
                    assertThat(navKey.fileName).isEqualTo(video.name)
                    assertThat(navKey.parentHandle).isEqualTo(video.parentId.longValue)
                    assertThat(navKey.fileHandle).isEqualTo(video.id.longValue)
                    assertThat(navKey.mediaQueueTitle).isEqualTo(expectedPlaylistUiEntity.title)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that onItemClicked when selection is empty and getNodeContentUriByHandleUseCase throws does not trigger navigation`() =
        runTest {
            val video = createVideoUiEntity(handle = 1L)
            stubInitialValues()
            whenever(getNodeContentUriByHandleUseCase(any())).thenAnswer {
                throw RuntimeException("test failure")
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistDetailUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemClicked(video)
                    advanceUntilIdle()

                    assertThat(underTest.navigateToVideoPlayerEvent.value)
                        .isEqualTo(consumed())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that resetNavigateToVideoPlayer resets event to consumed`() = runTest {
        val video = createVideoUiEntity(handle = 1L)
        val contentUri = NodeContentUri.RemoteContentUri("http://test.url", false)
        stubInitialValues()
        whenever(getNodeContentUriByHandleUseCase(video.id.longValue)).thenReturn(contentUri)

        underTest.uiState
            .filterIsInstance<VideoPlaylistDetailUiState.Data>()
            .test {
                awaitItem()
                underTest.onItemClicked(video)
                advanceUntilIdle()
                assertThat(underTest.navigateToVideoPlayerEvent.value)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)

                underTest.resetNavigateToVideoPlayer()
                assertThat(underTest.navigateToVideoPlayerEvent.value).isEqualTo(consumed())
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that removeVideosFromPlaylist invokes use case and triggers numberOfRemovedVideosEvent`() =
        runTest {
            stubInitialValues()
            val elementIds = listOf(10L, 20L)
            val numberOfRemoved = 2
            whenever(removeVideosFromPlaylistUseCase(any(), any())).thenReturn(numberOfRemoved)

            underTest.removeVideosFromPlaylist(elementIds)
            advanceUntilIdle()

            val event = underTest.videoPlaylistEditState.value.numberOfRemovedVideosEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((event as StateEventWithContentTriggered).content).isEqualTo(numberOfRemoved)
            verify(removeVideosFromPlaylistUseCase).invoke(testId, elementIds)
        }

    @Test
    fun `test that removeVideosFromPlaylist with custom handle invokes use case with that handle`() =
        runTest {
            stubInitialValues()
            val customHandle = 999L
            val elementIds = listOf(1L)
            whenever(removeVideosFromPlaylistUseCase(any(), any())).thenReturn(1)

            underTest.removeVideosFromPlaylist(elementIds, handle = customHandle)
            advanceUntilIdle()

            verify(removeVideosFromPlaylistUseCase).invoke(NodeId(customHandle), elementIds)
        }

    @Test
    fun `test that resetNumberOfRemovedVideosEvent resets event to consumed`() = runTest {
        stubInitialValues()
        whenever(removeVideosFromPlaylistUseCase(any(), any())).thenReturn(1)

        underTest.removeVideosFromPlaylist(listOf(1L))
        advanceUntilIdle()
        assertThat(underTest.videoPlaylistEditState.value.numberOfRemovedVideosEvent)
            .isInstanceOf(StateEventWithContentTriggered::class.java)

        underTest.resetNumberOfRemovedVideosEvent()
        assertThat(
            underTest.videoPlaylistEditState.value.numberOfRemovedVideosEvent
        ).isEqualTo(consumed())
    }

    @Test
    fun `test that when removeVideosFromPlaylistUseCase fails numberOfRemovedVideosEvent is not triggered`() =
        runTest {
            stubInitialValues()
            whenever(
                removeVideosFromPlaylistUseCase(
                    any(),
                    any()
                )
            ).thenThrow(RuntimeException("test"))

            underTest.removeVideosFromPlaylist(listOf(1L))
            advanceUntilIdle()

            assertThat(
                underTest.videoPlaylistEditState.value.numberOfRemovedVideosEvent
            ).isEqualTo(consumed())
        }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
        duration: Duration = 1.minutes,
        elementID: Long? = 1L,
        isSharedItems: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = VideoUiEntity(
        id = NodeId(handle),
        name = name,
        parentId = NodeId(parentHandle),
        elementID = elementID,
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
        playlistUiEntity: VideoPlaylistUiEntity,
        videos: List<TypedVideoNode>? = null,
    ) =
        UserVideoPlaylist(
            id = playlistUiEntity.id,
            title = playlistUiEntity.title,
            cover = null,
            creationTime = playlistUiEntity.creationTime,
            modificationTime = playlistUiEntity.modificationTime,
            thumbnailList = playlistUiEntity.thumbnailList,
            numberOfVideos = videos?.size ?: playlistUiEntity.numberOfVideos,
            totalDuration = 0.minutes,
            videos = videos ?: emptyList()
        )

    private fun createTypedVideoNode(videoUiEntity: VideoUiEntity) = TypedVideoNode(
        fileNode = mock<FileNode> {
            on { id }.thenReturn(videoUiEntity.id)
            on { name }.thenReturn(videoUiEntity.name)
            on { parentId }.thenReturn(videoUiEntity.parentId)
            on { isMarkedSensitive }.thenReturn(videoUiEntity.isMarkedSensitive)
            on { isSensitiveInherited }.thenReturn(videoUiEntity.isSensitiveInherited)
        },
        duration = videoUiEntity.duration,
        elementID = null,
        isOutShared = videoUiEntity.isSharedItems,
        watchedTimestamp = 0L,
        collectionTitle = null,
    )
}