package mega.privacy.android.feature.photos.presentation.playlists

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.videosection.SystemVideoPlaylist
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistTitleValidationErrorMessageMapper
import mega.privacy.android.feature.photos.mapper.VideoPlaylistUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.minutes

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class VideoPlaylistsTabViewModelTest {
    private lateinit var underTest: VideoPlaylistsTabViewModel

    private val getVideoPlaylistsUseCase = mock<GetVideoPlaylistsUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorVideoPlaylistSetsUpdateUseCase =
        mock<MonitorVideoPlaylistSetsUpdateUseCase>()
    private val videoPlaylistUiEntityMapper = mock<VideoPlaylistUiEntityMapper>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val removeVideoPlaylistsUseCase = mock<RemoveVideoPlaylistsUseCase>()
    private val nodeSortConfigurationUiMapper = NodeSortConfigurationUiMapper()
    private val videoPlaylistTitleValidationErrorMessageMapper =
        mock<VideoPlaylistTitleValidationErrorMessageMapper>()
    private val updateVideoPlaylistTitleUseCase = mock<UpdateVideoPlaylistTitleUseCase>()
    private val createVideoPlaylistUseCase = mock<CreateVideoPlaylistUseCase>()
    private val getNextDefaultAlbumNameUseCase = mock<GetNextDefaultAlbumNameUseCase>()

    private val expectedId = NodeId(1L)
    private val expectedPlaylist = VideoPlaylistUiEntity(
        id = expectedId,
        title = "Playlist 1",
        cover = null,
        creationTime = 1L,
        modificationTime = 1L,
        thumbnailList = null,
        numberOfVideos = 0,
        totalDuration = "",
        isSelected = false,
        isSystemVideoPlayer = false
    )

    private val sortOrderFlow = MutableStateFlow(SortOrder.ORDER_MODIFICATION_DESC)

    @BeforeEach
    fun setUp() {
        underTest = VideoPlaylistsTabViewModel(
            getVideoPlaylistsUseCase = getVideoPlaylistsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorVideoPlaylistSetsUpdateUseCase = monitorVideoPlaylistSetsUpdateUseCase,
            videoPlaylistUiEntityMapper = videoPlaylistUiEntityMapper,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            removeVideoPlaylistsUseCase = removeVideoPlaylistsUseCase,
            videoPlaylistTitleValidationErrorMessageMapper = videoPlaylistTitleValidationErrorMessageMapper,
            updateVideoPlaylistTitleUseCase = updateVideoPlaylistTitleUseCase,
            createVideoPlaylistUseCase = createVideoPlaylistUseCase,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
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
            monitorSortCloudOrderUseCase,
            removeVideoPlaylistsUseCase,
            videoPlaylistTitleValidationErrorMessageMapper,
            updateVideoPlaylistTitleUseCase,
            createVideoPlaylistUseCase,
            getNextDefaultAlbumNameUseCase
        )
    }

    @Test
    fun `test that Data state is returned if values found`() = runTest {
        stubInitialValues()
        underTest.uiState
            .filterIsInstance<VideoPlaylistsTabUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.videoPlaylistEntities).isNotEmpty()
                assertThat(actual.selectedSortConfiguration).isEqualTo(
                    nodeSortConfigurationUiMapper(
                        sortOrderFlow.value
                    )
                )
                assertThat(actual.selectedPlaylists).isEmpty()
                assertThat(actual.query).isNull()
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that getVideoPlaylistsUseCase is invoked when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            val testNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 1.minutes))
            }
            stubInitialValues()
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))

            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.videoPlaylistEntities).isNotEmpty()
                    clearInvocations(getVideoPlaylistsUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verify(getVideoPlaylistsUseCase).invoke()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getVideoPlaylistsUseCase should not be invoked when no video nodes updated`() =
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
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.videoPlaylistEntities).isNotEmpty()
                    clearInvocations(getVideoPlaylistsUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verifyNoInteractions(getVideoPlaylistsUseCase)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getVideoPlaylistsUseCase is invoked when monitorVideoPlaylistSetsUpdateUseCase is triggered`() =
        runTest {
            stubInitialValues()
            val videoPlaylistSetsUpdateFlow = MutableStateFlow(emptyList<Long>())

            monitorVideoPlaylistSetsUpdateUseCase.stub {
                on { invoke() }.thenReturn(videoPlaylistSetsUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.videoPlaylistEntities).isNotEmpty()
                    clearInvocations(getVideoPlaylistsUseCase)

                    videoPlaylistSetsUpdateFlow.emit(listOf(1L))
                    verify(getVideoPlaylistsUseCase).invoke()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that uiState is correctly updated when setCloudSortOrder is invoked`() =
        runTest {
            val initialConfiguration = nodeSortConfigurationUiMapper(sortOrderFlow.value)

            stubInitialValues()
            val newConfiguration = NodeSortConfiguration(
                NodeSortOption.Name,
                SortDirection.Ascending
            )
            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.videoPlaylistEntities).isNotEmpty()
                    assertThat(initial.videoPlaylistEntities.size).isEqualTo(2)
                    assertThat(initial.selectedSortConfiguration).isEqualTo(initialConfiguration)

                    underTest.setCloudSortOrder(newConfiguration)

                    val actual = awaitItem()
                    assertThat(actual.videoPlaylistEntities).isNotEmpty()
                    assertThat(actual.videoPlaylistEntities.size).isEqualTo(2)
                    assertThat(actual.selectedSortConfiguration).isEqualTo(newConfiguration)

                    cancelAndIgnoreRemainingEvents()
                }
        }

    @ParameterizedTest(name = "when sortOrder is {0}")
    @EnumSource(
        value = SortOrder::class,
        names = ["ORDER_LABEL_DESC", "ORDER_LABEL_ASC", "ORDER_FAV_DESC", "ORDER_FAV_ASC", "ORDER_SIZE_DESC", "ORDER_SIZE_ASC"]
    )
    fun `test that sortOrder is ORDER_DEFAULT_ASC when sortOrder is ORDER_LABEL_DESC`(
        sortOrder: SortOrder,
    ) =
        runTest {
            val initialConfiguration = nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)
            stubInitialValues()
            whenever(monitorSortCloudOrderUseCase()).thenReturn(
                flowOf(sortOrder)
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    val actual = awaitItem()
                    assertThat(actual.selectedSortConfiguration).isEqualTo(initialConfiguration)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedPlaylists are updated correctly`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            stubInitialValues(
                nodesAndEntities = mapOf(
                    createVideoPlaylist(playlist1) to playlist1,
                    createVideoPlaylist(playlist2) to playlist2
                )
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    skipItems(1)

                    underTest.onItemLongClicked(playlist1)
                    var actual = awaitItem()
                    assertThat(actual.selectedPlaylists).hasSize(1)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(playlist1.id)

                    underTest.onItemClicked(playlist2)
                    actual = awaitItem()
                    assertThat(actual.selectedPlaylists).hasSize(2)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(
                        playlist1.id,
                        playlist2.id
                    )
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedPlaylists are updated correctly after selectAllVideos is invoked`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            stubInitialValues(
                nodesAndEntities = mapOf(
                    createVideoPlaylist(playlist1) to playlist1,
                    createVideoPlaylist(playlist2) to playlist2
                )
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    var actual = awaitItem()
                    assertThat(actual.videoPlaylistEntities).isNotEmpty()
                    assertThat(actual.selectedPlaylists).isEmpty()

                    underTest.selectAllVideos()
                    actual = awaitItem()
                    assertThat(actual.selectedPlaylists).hasSize(2)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(
                        playlist1.id,
                        playlist2.id
                    )
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedPlaylists are updated correctly after clearSelection is invoked`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            stubInitialValues(
                nodesAndEntities = mapOf(
                    createVideoPlaylist(playlist1) to playlist1,
                    createVideoPlaylist(playlist2) to playlist2
                )
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    skipItems(1)
                    underTest.onItemLongClicked(playlist1)
                    var actual = awaitItem()
                    assertThat(actual.selectedPlaylists).hasSize(1)
                    assertThat(actual.selectedPlaylists.map { it.id }).containsExactly(playlist1.id)

                    underTest.clearSelection()
                    actual = awaitItem()
                    assertThat(actual.selectedPlaylists).isEmpty()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that playlistsRemovedEvent is updated correctly`() =
        runTest {
            val playlist1 = createVideoPlaylistUiEntity(handle = 1L)
            val playlist2 = createVideoPlaylistUiEntity(handle = 2L)

            stubInitialValues(
                nodesAndEntities = mapOf(
                    createVideoPlaylist(playlist1) to playlist1,
                    createVideoPlaylist(playlist2) to playlist2
                )
            )

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
    fun `test that uiState is correctly updated when searchQuery is invoked`() = runTest {
        val query = "query"
        stubInitialValues()
        underTest.uiState
            .filterIsInstance<VideoPlaylistsTabUiState.Data>()
            .test {
                val initial = awaitItem()
                // Verify initial state has no query
                assertThat(initial.query).isNull()

                underTest.searchQuery(query)
                val actual = awaitItem()
                assertThat(actual.videoPlaylistEntities).isNotEmpty()
                assertThat(actual.query).isEqualTo(query)
                cancelAndIgnoreRemainingEvents()
            }
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
    fun `test that showUpdateVideoPlaylist updated correctly`() = runTest {
        underTest.videoPlaylistEditState.test {
            assertThat(awaitItem().showUpdateVideoPlaylist).isFalse()

            underTest.showUpdateVideoPlaylistDialog()
            assertThat(awaitItem().showUpdateVideoPlaylist).isTrue()

            underTest.resetShowUpdateVideoPlaylist()
            assertThat(awaitItem().showUpdateVideoPlaylist).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that showCreateVideoPlaylistDialog sets showCreateVideoPlaylist to true`() =
        runTest {
            underTest.videoPlaylistEditState.test {
                assertThat(awaitItem().showCreateVideoPlaylist).isFalse()

                underTest.showCreateVideoPlaylistDialog()
                assertThat(awaitItem().showCreateVideoPlaylist).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetShowCreateVideoPlaylist sets showCreateVideoPlaylist to false`() =
        runTest {
            underTest.showCreateVideoPlaylistDialog()
            advanceUntilIdle()

            underTest.videoPlaylistEditState.test {
                assertThat(awaitItem().showCreateVideoPlaylist).isTrue()

                underTest.resetShowCreateVideoPlaylist()
                assertThat(awaitItem().showCreateVideoPlaylist).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that getPresetNewVideoPlaylistTitle returns result from getNextDefaultAlbumNameUseCase`() =
        runTest {
            val placeholderTitle = "New playlist"
            val expectedTitle = "New playlist (1)"
            stubInitialValues()
            val currentNames = listOf(expectedPlaylist.title, expectedPlaylist.title)
            whenever(
                getNextDefaultAlbumNameUseCase(
                    defaultName = placeholderTitle,
                    currentNames = currentNames
                )
            ).thenReturn(expectedTitle)

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

            val result = underTest.getPresetNewVideoPlaylistTitle(placeholderTitle)

            assertThat(result).isEqualTo(expectedTitle)
            verify(getNextDefaultAlbumNameUseCase).invoke(
                defaultName = placeholderTitle,
                currentNames = currentNames
            )
        }

    @Test
    fun `test that getPresetNewVideoPlaylistTitle returns empty string when use case throws`() =
        runTest {
            stubInitialValues()
            whenever(getNextDefaultAlbumNameUseCase(any(), any())).thenThrow(
                RuntimeException("test")
            )

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

            val result = underTest.getPresetNewVideoPlaylistTitle("New playlist")

            assertThat(result).isEmpty()
        }

    @Test
    fun `test that getPresetNewVideoPlaylistTitle passes empty currentNames when no playlists exist`() =
        runTest {
            val placeholderTitle = "New playlist"
            whenever(getNextDefaultAlbumNameUseCase(placeholderTitle, emptyList()))
                .thenReturn(placeholderTitle)
            stubInitialValues(nodesAndEntities = emptyMap())

            underTest.uiState
                .filterIsInstance<VideoPlaylistsTabUiState.Data>()
                .test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

            val result = underTest.getPresetNewVideoPlaylistTitle(placeholderTitle)

            assertThat(result).isEqualTo(placeholderTitle)
            verify(getNextDefaultAlbumNameUseCase).invoke(
                defaultName = placeholderTitle,
                currentNames = emptyList()
            )
        }

    @Test
    fun `test that createNewPlaylist invokes use case and triggers success event on UserVideoPlaylist`() =
        runTest {
            val title = " My New Playlist "
            val trimmedTitle = "My New Playlist"
            val createdPlaylist = createVideoPlaylist(expectedPlaylist)
            stubInitialValues()
            whenever(createVideoPlaylistUseCase(trimmedTitle)).thenReturn(createdPlaylist)

            underTest.createNewPlaylist(title)
            advanceUntilIdle()

            underTest.videoPlaylistEditState.test {
                val actual = awaitItem()
                assertThat(actual.createVideoPlaylistSuccessEvent).isEqualTo(
                    triggered(createdPlaylist)
                )
                cancelAndIgnoreRemainingEvents()
            }
            verify(createVideoPlaylistUseCase).invoke(trimmedTitle)
        }

    @Test
    fun `test that createNewPlaylist sets error message when PlaylistNameValidationException is thrown`() =
        runTest {
            val title = "Duplicate"
            val errorMessage = "Playlist already exists"
            whenever(createVideoPlaylistUseCase(any())).thenAnswer {
                throw PlaylistNameValidationException.Exists
            }
            whenever(
                videoPlaylistTitleValidationErrorMessageMapper(
                    PlaylistNameValidationException.Exists
                )
            ).thenReturn(errorMessage)

            underTest.createNewPlaylist(title)
            advanceUntilIdle()

            underTest.videoPlaylistEditState.test {
                assertThat(awaitItem().editVideoPlaylistErrorMessage).isEqualTo(errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that createNewPlaylist does not trigger success event when use case returns non-UserVideoPlaylist`() =
        runTest {
            whenever(createVideoPlaylistUseCase(any())).thenReturn(mock<SystemVideoPlaylist>())

            underTest.createNewPlaylist("New Playlist")
            advanceUntilIdle()

            underTest.videoPlaylistEditState.test {
                assertThat(awaitItem().createVideoPlaylistSuccessEvent).isEqualTo(consumed())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetCreateVideoPlaylistSuccessEvent consumes createVideoPlaylistSuccessEvent`() =
        runTest {
            val createdPlaylist = createVideoPlaylist(expectedPlaylist)
            stubInitialValues()
            whenever(createVideoPlaylistUseCase(any())).thenReturn(createdPlaylist)

            underTest.createNewPlaylist("New Playlist")
            advanceUntilIdle()

            underTest.videoPlaylistEditState.test {
                assertThat(awaitItem().createVideoPlaylistSuccessEvent).isEqualTo(
                    triggered(createdPlaylist)
                )

                underTest.resetCreateVideoPlaylistSuccessEvent()
                assertThat(awaitItem().createVideoPlaylistSuccessEvent).isEqualTo(consumed())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun stubInitialValues(
        nodesAndEntities: Map<VideoPlaylist, VideoPlaylistUiEntity> = mapOf(
            mock<VideoPlaylist>() to expectedPlaylist,
            mock<VideoPlaylist>() to expectedPlaylist
        ),
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

        setCloudSortOrderUseCase.stub {
            onBlocking { invoke(any()) }.thenAnswer { invocation -> sortOrderFlow.tryEmit(invocation.arguments[0] as SortOrder) }
        }

        whenever(monitorSortCloudOrderUseCase()).thenReturn(
            sortOrderFlow
        )
        whenever(getVideoPlaylistsUseCase()).thenReturn(nodesAndEntities.keys.toList())

        whenever(
            videoPlaylistUiEntityMapper(any())
        ).thenAnswer { invocation -> nodesAndEntities[invocation.arguments[0]] }
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