package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.mapper.SelectVideoItemUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class SelectVideosForPlaylistViewModelTest {
    private lateinit var underTest: SelectVideosForPlaylistViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val getNodesByIdInChunkUseCase = mock<GetNodesByIdInChunkUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val selectVideoItemUiEntityMapper = mock<SelectVideoItemUiEntityMapper>()
    private val nodeSortConfigurationUiMapper = NodeSortConfigurationUiMapper()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val setViewTypeUseCase = mock<SetViewType>()
    private val monitorViewTypeUseCase = mock<MonitorViewType>()
    private val addVideosToPlaylistUseCase = mock<AddVideosToPlaylistUseCase>()

    private val rootNodeId = NodeId(0L)
    private val folderNodeId = NodeId(100L)
    private val folderName = "Folder Name"
    private val playlistHandle = 200L
    private val videoEntity = SelectVideoItemUiEntity(
        id = NodeId(1L),
        name = "video.mp4",
        title = LocalizedText.Literal("video.mp4"),
        isFolder = false,
    )
    private val folderEntity = SelectVideoItemUiEntity(
        id = folderNodeId,
        name = folderName,
        title = LocalizedText.Literal(folderName),
        isFolder = true,
    )

    @BeforeEach
    fun setUp() {
        monitorNodeUpdatesUseCase.stub {
            onBlocking { invoke() }.thenReturn(flow { awaitCancellation() })
        }
        monitorSortCloudOrderUseCase.stub {
            onBlocking { invoke() }.thenReturn(
                flow {
                    emit(SortOrder.ORDER_DEFAULT_ASC)
                    awaitCancellation()
                }
            )
        }
        monitorViewTypeUseCase.stub {
            onBlocking { invoke() }.thenReturn(
                flow {
                    emit(ViewType.LIST)
                    awaitCancellation()
                }
            )
        }
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getRootNodeIdUseCase,
            getNodesByIdInChunkUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase,
            monitorSortCloudOrderUseCase,
            selectVideoItemUiEntityMapper,
            setCloudSortOrderUseCase,
            setViewTypeUseCase,
            monitorViewTypeUseCase,
            addVideosToPlaylistUseCase,
        )
    }

    private suspend fun stubInitialValues(
        nodeHandle: Long = folderNodeId.longValue,
        nodeName: String? = folderName,
        playlistHandle: Long = this.playlistHandle,
        nodesWithHasMore: Pair<List<TypedNode>, Boolean> = emptyList<TypedNode>() to false,
        isHiddenNodesEnabled: Boolean = true,
        showHiddenItems: Boolean = false,
        viewType: ViewType = ViewType.LIST,
    ) {
        if (nodeHandle == -1L) {
            whenever(getRootNodeIdUseCase()).thenReturn(rootNodeId)
        }
        whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
            flow {
                emit(nodesWithHasMore)
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
        whenever(monitorViewTypeUseCase()).thenReturn(
            flow {
                emit(viewType)
                awaitCancellation()
            }
        )
        underTest = SelectVideosForPlaylistViewModel(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            selectVideoItemUiEntityMapper = selectVideoItemUiEntityMapper,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            setViewTypeUseCase = setViewTypeUseCase,
            monitorViewTypeUseCase = monitorViewTypeUseCase,
            addVideosToPlaylistUseCase = addVideosToPlaylistUseCase,
            nodeHandle = nodeHandle,
            nodeName = nodeName,
            playlistHandle = playlistHandle,
        )
    }

    @Test
    fun `test that when nodeHandle is minus one isCloudDriveRoot is true and getRootNodeIdUseCase is used`() =
        runTest {
            stubInitialValues(
                nodeHandle = -1L,
                nodeName = "Cloud Drive",
            )

            underTest.uiState
                .filterIsInstance<SelectVideosForPlaylistUiState.Data>()
                .test {
                    val data = awaitItem()
                    assertThat(data.isCloudDriveRoot).isTrue()
                    assertThat(data.title).isEqualTo(LocalizedText.Literal("Cloud Drive"))
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that itemClicked with folder item when selection empty triggers navigateToFolderEvent`() =
        runTest {
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                awaitItem()
                underTest.itemClicked(folderEntity)
                advanceUntilIdle()

                val event = underTest.navigateToFolderEvent.value
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (event as StateEventWithContentTriggered).content
                assertThat(content.first).isEqualTo(playlistHandle)
                assertThat(content.second).isEqualTo(folderEntity)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetNavigateToFolderEvent resets event to consumed`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            awaitItem()
            underTest.itemClicked(folderEntity)
            advanceUntilIdle()
            assertThat(underTest.navigateToFolderEvent.value).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )

            underTest.resetNavigateToFolderEvent()
            assertThat(underTest.navigateToFolderEvent.value).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that itemClicked with folder when selection not empty does not trigger navigateToFolderEvent`() =
        runTest {
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                awaitItem()
                underTest.itemClicked(videoEntity)
                advanceUntilIdle()
                assertThat(awaitItem().selectItemHandles).containsExactly(videoEntity.id.longValue)

                underTest.itemClicked(folderEntity)
                advanceUntilIdle()
                assertThat(underTest.navigateToFolderEvent.value).isEqualTo(consumed())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that itemClicked with file item toggles selection and does not trigger navigateToFolderEvent`() =
        runTest {
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                val initial = awaitItem()
                assertThat(initial.selectItemHandles).isEmpty()

                underTest.itemClicked(videoEntity)
                advanceUntilIdle()
                assertThat(awaitItem().selectItemHandles).containsExactly(videoEntity.id.longValue)
                assertThat(underTest.navigateToFolderEvent.value).isEqualTo(consumed())

                underTest.itemClicked(videoEntity)
                advanceUntilIdle()
                assertThat(awaitItem().selectItemHandles).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that nodesLoadingState is PartiallyLoaded when hasMore is true`() = runTest {
        val typedVideo = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("v.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        whenever(selectVideoItemUiEntityMapper(any())).thenReturn(videoEntity)
        stubInitialValues(
            nodeHandle = -1L,
            nodeName = "",
            nodesWithHasMore = listOf<TypedNode>(typedVideo) to true,
        )

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            val data = awaitItem()
            assertThat(data.nodesLoadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nodesLoadingState is FullyLoaded when hasMore is false`() = runTest {
        stubInitialValues(
            nodeHandle = -1L,
            nodeName = "",
        )

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            val data = awaitItem()
            assertThat(data.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when showHiddenItems is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that uiState showHiddenItems is updated correctly from getShowHiddenItemsFlow`(
        showHiddenItems: Boolean,
    ) = runTest {
        stubInitialValues(showHiddenItems = showHiddenItems)

        underTest.uiState
            .filterIsInstance<SelectVideosForPlaylistUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.showHiddenItems).isEqualTo(showHiddenItems)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @ParameterizedTest(name = "when isHiddenNodesEnabled is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that uiState showHiddenItems equals showHiddenItems or not isHiddenNodesEnabled from getShowHiddenItemsFlow`(
        isHiddenNodesEnabled: Boolean,
    ) = runTest {
        stubInitialValues(
            isHiddenNodesEnabled = isHiddenNodesEnabled,
            showHiddenItems = false,
        )

        underTest.uiState
            .filterIsInstance<SelectVideosForPlaylistUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.showHiddenItems).isEqualTo(!isHiddenNodesEnabled)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that searchQuery updates query in uiState`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            val initial = awaitItem()
            assertThat(initial.query).isNull()

            underTest.searchQuery("test")
            advanceUntilIdle()
            val afterSearch = awaitItem()
            assertThat(afterSearch.query).isEqualTo("test")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are filtered by query`() = runTest {
        val testName = "match.mp4"
        val typedVideo = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn(testName)
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        val testVideoEntity = SelectVideoItemUiEntity(
            id = NodeId(1L),
            name = testName,
            title = LocalizedText.Literal(testName),
            isFolder = false,
        )
        whenever(selectVideoItemUiEntityMapper(any())).thenReturn(testVideoEntity)
        stubInitialValues(
            nodesWithHasMore = listOf<TypedNode>(typedVideo) to false,
        )

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            val initial = awaitItem()
            assertThat(initial.items).hasSize(1)

            underTest.searchQuery("match")
            advanceUntilIdle()
            assertThat(awaitItem().items).hasSize(1)

            underTest.searchQuery("nomatch")
            advanceUntilIdle()
            assertThat(awaitItem().items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that setCloudSortOrder invokes setCloudSortOrderUseCase`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            awaitItem()
            val config = NodeSortConfiguration.default
            underTest.setCloudSortOrder(config)
            advanceUntilIdle()
            verify(setCloudSortOrderUseCase).invoke(any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that changeViewTypeClicked invokes setViewTypeUseCase with toggled view type`() =
        runTest {
            stubInitialValues(viewType = ViewType.LIST)

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                awaitItem()
                underTest.changeViewTypeClicked()
                advanceUntilIdle()
                verify(setViewTypeUseCase).invoke(ViewType.GRID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that changeViewTypeClicked when current is GRID invokes setViewTypeUseCase with LIST`() =
        runTest {
            stubInitialValues(viewType = ViewType.GRID)

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                awaitItem()
                underTest.changeViewTypeClicked()
                advanceUntilIdle()
                verify(setViewTypeUseCase).invoke(ViewType.LIST)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "when viewType is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that uiState currentViewType is from monitorViewTypeUseCase`(
        useGrid: Boolean,
    ) = runTest {
        val viewType = if (useGrid) ViewType.GRID else ViewType.LIST
        stubInitialValues(viewType = viewType)

        underTest.uiState
            .filterIsInstance<SelectVideosForPlaylistUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.currentViewType).isEqualTo(viewType)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that selectItemHandles updates when file item clicked and clears when clearSelection called`() =
        runTest {
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                awaitItem()
                underTest.itemClicked(videoEntity)
                advanceUntilIdle()
                assertThat(awaitItem().selectItemHandles).containsExactly(videoEntity.id.longValue)

                underTest.clearSelection()
                advanceUntilIdle()
                assertThat(awaitItem().selectItemHandles).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that addVideosToPlaylist invokes use case and triggers numberOfAddedVideosEvent`() =
        runTest {
            whenever(addVideosToPlaylistUseCase(any(), any())).thenReturn(2)
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
                awaitItem()
                underTest.itemClicked(videoEntity)
                advanceUntilIdle()
                underTest.addVideosToPlaylist(videoIDs = listOf(videoEntity.id))
                advanceUntilIdle()

                val event = underTest.numberOfAddedVideosEvent.value
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((event as StateEventWithContentTriggered).content).isEqualTo(
                    playlistHandle to 2
                )
                verify(addVideosToPlaylistUseCase).invoke(any(), any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetNumberOfAddedVideosEvent resets event to consumed`() = runTest {
        whenever(addVideosToPlaylistUseCase(any(), any())).thenReturn(1)
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideosForPlaylistUiState.Data>().test {
            awaitItem()
            underTest.addVideosToPlaylist(videoIDs = listOf(videoEntity.id))
            advanceUntilIdle()
            assertThat(underTest.numberOfAddedVideosEvent.value).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )

            underTest.resetNumberOfAddedVideosEvent()
            assertThat(underTest.numberOfAddedVideosEvent.value).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
