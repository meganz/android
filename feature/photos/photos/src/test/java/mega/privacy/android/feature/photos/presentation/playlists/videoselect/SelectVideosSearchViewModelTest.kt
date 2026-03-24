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
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
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
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
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
class SelectVideosSearchViewModelTest {
    private lateinit var underTest: SelectVideosSearchViewModel

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
        isVideo = true,
    )
    private val videoEntity2 = SelectVideoItemUiEntity(
        id = NodeId(2L),
        name = "video2.mp4",
        title = LocalizedText.Literal("video2.mp4"),
        isFolder = false,
        isVideo = true,
    )
    private val folderEntity = SelectVideoItemUiEntity(
        id = folderNodeId,
        name = folderName,
        title = LocalizedText.Literal(folderName),
        isFolder = true,
        isVideo = false,
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
        val args = SelectVideosSearchViewModel.Args(
            nodeHandle = nodeHandle,
            nodeName = nodeName,
            playlistHandle = playlistHandle,
            isNewlyCreated = false,
        )
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
        underTest = SelectVideosSearchViewModel(
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
            args = args,
        )
    }

    @Test
    fun `test that when nodeHandle is minus one isCloudDriveRoot is true`() = runTest {
        stubInitialValues(nodeHandle = -1L, nodeName = "Cloud Drive")

        underTest.uiState
            .filterIsInstance<SelectVideoSearchUiState.Data>()
            .test {
                val data = awaitItem()
                assertThat(data.isCloudDriveRoot).isTrue()
                assertThat(data.title).isEqualTo(LocalizedText.Literal("Cloud Drive"))
                assertThat(data.items).isEmpty()
                assertThat(data.searchText).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that placeholderText uses folder string when node name is set`() = runTest {
        stubInitialValues(nodeHandle = folderNodeId.longValue, nodeName = folderName)

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            val data = awaitItem()
            assertThat(data.placeholderText).isEqualTo(
                LocalizedText.StringRes(
                    resId = sharedR.string.search_placeholder_folder,
                    formatArgs = listOf(folderName),
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that searchQuery updates searchText and searchedQuery in uiState`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.searchQuery("find")
            advanceUntilIdle()
            val after = awaitItem()
            assertThat(after.searchText).isEqualTo("find")
            assertThat(after.searchedQuery).isEqualTo("find")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that items are filtered by search query`() = runTest {
        val typedVideo = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("match.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        whenever(selectVideoItemUiEntityMapper(any())).thenReturn(videoEntity)
        stubInitialValues(
            nodesWithHasMore = listOf<TypedNode>(typedVideo) to false,
        )

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
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
    fun `test that nodesLoadingState is FullyLoaded when query is empty`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            val data = awaitItem()
            assertThat(data.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            assertThat(data.items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that itemClicked with folder when selection empty triggers navigateToFolderEvent`() =
        runTest {
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("x")
                advanceUntilIdle()
                awaitItem()
                underTest.itemClicked(folderEntity)
                advanceUntilIdle()

                val event = underTest.navigateToFolderEvent.value
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (event as StateEventWithContentTriggered).content
                assertThat(content).isEqualTo(folderEntity)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetNavigateToFolderEvent resets event to consumed`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.searchQuery("x")
            advanceUntilIdle()
            awaitItem()
            underTest.itemClicked(folderEntity)
            advanceUntilIdle()
            assertThat(underTest.navigateToFolderEvent.value).isInstanceOf(
                StateEventWithContentTriggered::class.java,
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

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("v")
                advanceUntilIdle()
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
    fun `test that itemClicked with file toggles selection and does not trigger navigateToFolderEvent`() =
        runTest {
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("v")
                advanceUntilIdle()
                awaitItem()

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

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.searchQuery("v")
            advanceUntilIdle()
            assertThat(awaitItem().nodesLoadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nodesLoadingState is FullyLoaded when hasMore is false after search`() =
        runTest {
            val typedVideo = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { name }.thenReturn("v.mp4")
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            whenever(selectVideoItemUiEntityMapper(any())).thenReturn(videoEntity)
            stubInitialValues(
                nodesWithHasMore = listOf<TypedNode>(typedVideo) to false,
            )

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("v")
                advanceUntilIdle()
                assertThat(awaitItem().nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
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
            .filterIsInstance<SelectVideoSearchUiState.Data>()
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
            .filterIsInstance<SelectVideoSearchUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.showHiddenItems).isEqualTo(!isHiddenNodesEnabled)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that setCloudSortOrder invokes setCloudSortOrderUseCase`() = runTest {
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
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

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
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

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
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
            .filterIsInstance<SelectVideoSearchUiState.Data>()
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

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("v")
                advanceUntilIdle()
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
    fun `test that selectAll selects all video items and excludes folders`() = runTest {
        val typedVideo1 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("v1.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        val typedVideo2 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { name }.thenReturn("v2.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        val typedFolder = mock<TypedFolderNode> {
            on { id }.thenReturn(folderNodeId)
            on { name }.thenReturn(folderName)
        }
        whenever(selectVideoItemUiEntityMapper(typedVideo1)).thenReturn(videoEntity)
        whenever(selectVideoItemUiEntityMapper(typedVideo2)).thenReturn(videoEntity2)
        whenever(selectVideoItemUiEntityMapper(typedFolder)).thenReturn(folderEntity)
        stubInitialValues(
            nodesWithHasMore = listOf<TypedNode>(typedVideo1, typedVideo2, typedFolder) to false,
        )

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.searchQuery("v")
            advanceUntilIdle()
            val withItems = awaitItem()
            assertThat(withItems.items).hasSize(2)
            assertThat(withItems.selectItemHandles).isEmpty()

            underTest.selectAll()
            advanceUntilIdle()

            val afterSelectAll = awaitItem()
            assertThat(afterSelectAll.selectItemHandles).containsExactly(
                videoEntity.id.longValue,
                videoEntity2.id.longValue,
            )
            assertThat(afterSelectAll.selectItemHandles).doesNotContain(folderNodeId.longValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that selectAll selects only items that are video and not folder`() = runTest {
        val nonVideoEntity = SelectVideoItemUiEntity(
            id = NodeId(3L),
            name = "file.txt",
            title = LocalizedText.Literal("file.txt"),
            isFolder = false,
            isVideo = false,
        )
        val typedNode1 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("v.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        val typedNode2 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { name }.thenReturn("file.txt")
        }
        whenever(selectVideoItemUiEntityMapper(typedNode1)).thenReturn(videoEntity)
        whenever(selectVideoItemUiEntityMapper(typedNode2)).thenReturn(nonVideoEntity)
        stubInitialValues(
            nodesWithHasMore = listOf<TypedNode>(typedNode1, typedNode2) to false,
        )

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            // Both names contain "." so both nodes appear in search results
            underTest.searchQuery(".")
            advanceUntilIdle()
            skipItems(1)
            underTest.selectAll()
            advanceUntilIdle()

            val state = awaitItem()
            assertThat(state.selectItemHandles).containsExactly(videoEntity.id.longValue)
            assertThat(state.selectItemHandles).doesNotContain(nonVideoEntity.id.longValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that selectAll results in empty selection when no video items in state`() =
        runTest {
            val folderNode = mock<TypedFolderNode> {
                on { name }.thenReturn("FolderOnly")
            }
            whenever(selectVideoItemUiEntityMapper(any())).thenReturn(folderEntity)
            stubInitialValues(
                nodesWithHasMore = listOf<TypedNode>(folderNode) to false,
            )
            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("Folder")
                advanceUntilIdle()
                awaitItem()
            }
            underTest.selectAll()
            advanceUntilIdle()

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                assertThat(awaitItem().selectItemHandles).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that areAllSelected is true after selectAll when all items are videos`() = runTest {
        val typedVideo1 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("v1.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        val typedVideo2 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { name }.thenReturn("v2.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        whenever(selectVideoItemUiEntityMapper(typedVideo1)).thenReturn(videoEntity)
        whenever(selectVideoItemUiEntityMapper(typedVideo2)).thenReturn(videoEntity2)
        stubInitialValues(
            nodesWithHasMore = listOf<TypedNode>(typedVideo1, typedVideo2) to false,
        )

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.searchQuery("v")
            advanceUntilIdle()
            skipItems(1)
            underTest.selectAll()
            advanceUntilIdle()

            assertThat(awaitItem().areAllSelected).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that areAllSelected is false when only some items are selected`() = runTest {
        val typedVideo1 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { name }.thenReturn("v1.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        val typedVideo2 = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { name }.thenReturn("v2.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
        }
        whenever(selectVideoItemUiEntityMapper(typedVideo1)).thenReturn(videoEntity)
        whenever(selectVideoItemUiEntityMapper(typedVideo2)).thenReturn(videoEntity2)
        stubInitialValues(
            nodesWithHasMore = listOf<TypedNode>(typedVideo1, typedVideo2) to false,
        )

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.searchQuery("v")
            advanceUntilIdle()
            val initial = awaitItem()
            assertThat(initial.areAllSelected).isFalse()

            underTest.itemClicked(videoEntity)
            advanceUntilIdle()

            assertThat(awaitItem().areAllSelected).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that selectAll does not emit new state when all videos are already selected`() =
        runTest {
            val typedVideo1 = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { name }.thenReturn("v1.mp4")
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            val typedVideo2 = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(2L))
                on { name }.thenReturn("v2.mp4")
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            whenever(selectVideoItemUiEntityMapper(typedVideo1)).thenReturn(videoEntity)
            whenever(selectVideoItemUiEntityMapper(typedVideo2)).thenReturn(videoEntity2)
            stubInitialValues(
                nodesWithHasMore = listOf<TypedNode>(typedVideo1, typedVideo2) to false,
            )

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("v")
                advanceUntilIdle()
                skipItems(1)
                underTest.selectAll()
                advanceUntilIdle()
                assertThat(awaitItem().areAllSelected).isTrue()

                underTest.selectAll()
                advanceUntilIdle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that addVideosToPlaylist invokes use case and triggers numberOfAddedVideosEvent`() =
        runTest {
            whenever(addVideosToPlaylistUseCase(any(), any())).thenReturn(2)
            stubInitialValues()

            underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
                awaitItem()
                underTest.searchQuery("v")
                advanceUntilIdle()
                awaitItem()
                underTest.itemClicked(videoEntity)
                advanceUntilIdle()
                underTest.addVideosToPlaylist(videoIDs = listOf(videoEntity.id))
                advanceUntilIdle()

                val event = underTest.numberOfAddedVideosEvent.value
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((event as StateEventWithContentTriggered).content).isEqualTo(2)
                verify(addVideosToPlaylistUseCase).invoke(any(), any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resetNumberOfAddedVideosEvent resets event to consumed`() = runTest {
        whenever(addVideosToPlaylistUseCase(any(), any())).thenReturn(1)
        stubInitialValues()

        underTest.uiState.filterIsInstance<SelectVideoSearchUiState.Data>().test {
            awaitItem()
            underTest.addVideosToPlaylist(videoIDs = listOf(videoEntity.id))
            advanceUntilIdle()
            assertThat(underTest.numberOfAddedVideosEvent.value).isInstanceOf(
                StateEventWithContentTriggered::class.java,
            )

            underTest.resetNumberOfAddedVideosEvent()
            assertThat(underTest.numberOfAddedVideosEvent.value).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
