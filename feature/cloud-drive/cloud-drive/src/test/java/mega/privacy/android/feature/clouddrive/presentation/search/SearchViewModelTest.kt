package mega.privacy.android.feature.clouddrive.presentation.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeInfo
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.SearchPlaceholderMapper
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterResult
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchViewModelTest {
    private val searchUseCase: SearchUseCase = mock()
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase = mock()
    private val nodeUiItemMapper: NodeUiItemMapper = mock()
    private val typeFilterToSearchMapper: TypeFilterToSearchMapper = mock()
    private val setCloudSortOrderUseCase: SetCloudSortOrder = mock()
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper = mock()
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase = mock()
    private val setViewTypeUseCase: SetViewType = mock()
    private val monitorViewTypeUseCase: MonitorViewType = mock()
    private val nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper = mock()
    private val searchPlaceholderMapper: SearchPlaceholderMapper = mock()
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase = mock()
    private val nodeSourceType = NodeSourceType.CLOUD_DRIVE
    private val parentHandle = 123L
    private val args = SearchViewModel.Args(
        parentHandle = parentHandle,
        nodeSourceType = nodeSourceType
    )

    @AfterEach
    fun tearDown() {
        reset(
            searchUseCase,
            cancelCancelTokenUseCase,
            nodeUiItemMapper,
            monitorNodeUpdatesByIdUseCase,
            nodeSourceTypeToSearchTargetMapper
        )
    }

    private fun createViewModel(
        args: SearchViewModel.Args = this.args,
    ) = SearchViewModel(
        args = args,
        searchUseCase = searchUseCase,
        cancelCancelTokenUseCase = cancelCancelTokenUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        typeFilterToSearchMapper = typeFilterToSearchMapper,
        setCloudSortOrderUseCase = setCloudSortOrderUseCase,
        nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
        monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
        setViewTypeUseCase = setViewTypeUseCase,
        monitorViewTypeUseCase = monitorViewTypeUseCase,
        monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
        monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
        nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
        searchPlaceholderMapper = searchPlaceholderMapper,
        getNodeInfoByIdUseCase = getNodeInfoByIdUseCase,
    )

    private fun setupTestData(
        items: List<TypedNode> = emptyList(),
    ) {
        val nodeUiItems = items.map { node ->
            NodeUiItem(
                node = node,
                isSelected = false
            )
        }
        runBlocking {
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = anyOrNull(),
                    nodeSourceType = any(),
                    isPublicNodes = any(),
                    showPublicLinkCreationTime = any(),
                    highlightedNodeId = anyOrNull(),
                    highlightedNames = anyOrNull(),
                    isContactVerificationOn = any(),
                )
            ).thenReturn(nodeUiItems)
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(items)
        }

        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(SortOrder.ORDER_DEFAULT_ASC))
        whenever(nodeSortConfigurationUiMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(
            NodeSortConfiguration.default
        )
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(parentHandle),
                nodeSourceType
            )
        ).thenReturn(flowOf())
        whenever(nodeSourceTypeToSearchTargetMapper(any())).thenReturn(SearchTarget.ROOT_NODES)
        runBlocking {
            whenever(getNodeInfoByIdUseCase(any())).thenReturn(null)
            whenever(
                searchPlaceholderMapper(
                    nodeSourceType = any(),
                    nodeName = anyOrNull()
                )
            ).thenReturn(LocalizedText.StringRes(sharedR.string.search_bar_placeholder_text))
        }
    }

    @Test
    fun `test that initial state has empty search text and idle loading state`() = runTest {
        val underTest = createViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.searchText).isEmpty()
            assertThat(state.searchedQuery).isEmpty()
            assertThat(state.items).isEmpty()
            assertThat(state.nodesLoadingState).isEqualTo(NodesLoadingState.Idle)
        }
    }

    @Test
    fun `test that UpdateSearchText action updates searchText immediately`() = runTest {
        val underTest = createViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.searchText).isEmpty()

            underTest.processAction(SearchUiAction.UpdateSearchText("test"))
            val updatedState = awaitItem()
            assertThat(updatedState.searchText).isEqualTo("test")
        }
    }

    @Test
    fun `test that search is performed after debounce time when query is not empty`() = runTest {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file.txt")
        }
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        setupTestData(listOf(typedFileNode))

        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.searchedQuery).isEqualTo("test")
            assertThat(state.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
        }
    }

    @Test
    fun `test that empty query clears items and sets state to idle`() = runTest {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file.txt")
        }
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        setupTestData(listOf(typedFileNode))

        val underTest = createViewModel()
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()
        reset(searchUseCase, nodeUiItemMapper, typeFilterToSearchMapper)
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
        setupTestData(listOf(typedFileNode))

        underTest.uiState.test {
            skipItems(1)
            underTest.processAction(SearchUiAction.UpdateSearchText("test"))
            skipItems(1)
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()
            val stateWithResults = awaitItem()
            assertThat(stateWithResults.items).hasSize(1)
            assertThat(stateWithResults.searchedQuery).isEqualTo("test")

            underTest.processAction(SearchUiAction.UpdateSearchText(""))
            val textClearedState = awaitItem()
            assertThat(textClearedState.searchText).isEmpty()

            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            val stateAfterClear = awaitItem()
            assertThat(stateAfterClear.searchText).isEmpty()
            assertThat(stateAfterClear.items).isEmpty()
            assertThat(stateAfterClear.searchedQuery).isEmpty()
            assertThat(stateAfterClear.nodesLoadingState).isEqualTo(NodesLoadingState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that cancelCancelTokenUseCase is called before performing search`() = runTest {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file.txt")
        }
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        setupTestData(emptyList())

        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.UpdateSearchText("test"))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

        verify(cancelCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that items are cleared when search fails with non-cancellation exception`() =
        runTest {
            whenever(
                searchUseCase(
                    parentHandle = any(),
                    nodeSourceType = any(),
                    searchParameters = any(),
                    isSingleActivityEnabled = any(),
                )
            ).thenThrow(RuntimeException("Search failed"))

            val underTest = createViewModel()

            underTest.processAction(SearchUiAction.UpdateSearchText("test"))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.items).isEmpty()
                assertThat(state.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
            }
        }

    @Test
    fun `test that multiple rapid updates only trigger one search after debounce`() = runTest {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file.txt")
        }
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        setupTestData(listOf(typedFileNode))

        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.UpdateSearchText("t"))
        advanceTimeBy(100)
        underTest.processAction(SearchUiAction.UpdateSearchText("te"))
        advanceTimeBy(100)
        underTest.processAction(SearchUiAction.UpdateSearchText("tes"))
        advanceTimeBy(100)
        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.searchText).isEqualTo("test")
            assertThat(state.searchedQuery).isEqualTo("test")
        }
    }

    @Test
    fun `test that typeFilterToSearchMapper is called with correct parameters when performing search`() =
        runTest {
            val query = "test query"
            val typeFilterOption = TypeFilterOption.Audio
            val expectedSearchCategory = SearchCategory.AUDIO

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(
                SearchCategory.ALL
            )

            setupTestData(emptyList())

            val underTest = createViewModel()
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()
            reset(typeFilterToSearchMapper)
            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(
                SearchCategory.ALL
            )

            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(typeFilterToSearchMapper, times(1)).invoke(
                typeFilterOption = null,
                nodeSourceType = nodeSourceType
            )
        }

    @Test
    fun `test that typeFilterToSearchMapper is called with selected type filter option`() =
        runTest {
            val query = "test query"
            val typeFilterOption = TypeFilterOption.Video
            val expectedSearchCategory = SearchCategory.VIDEO

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
            whenever(typeFilterToSearchMapper(typeFilterOption, nodeSourceType)).thenReturn(
                expectedSearchCategory
            )

            setupTestData(emptyList())

            val underTest = createViewModel()
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            underTest.processAction(
                SearchUiAction.SelectFilter(
                    SearchFilterResult.Type(
                        typeFilterOption
                    )
                )
            )
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(typeFilterToSearchMapper, times(1)).invoke(
                typeFilterOption = typeFilterOption,
                nodeSourceType = nodeSourceType
            )
        }

    @Test
    fun `test that SearchParameters is constructed correctly with type filter`() = runTest {
        val query = "test query"
        val typeFilterOption = TypeFilterOption.Documents
        val expectedSearchCategory = SearchCategory.DOCUMENTS

        whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
        whenever(typeFilterToSearchMapper(typeFilterOption, nodeSourceType)).thenReturn(
            expectedSearchCategory
        )

        setupTestData(emptyList())

        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText(query))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        underTest.processAction(SearchUiAction.SelectFilter(SearchFilterResult.Type(typeFilterOption)))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        verify(searchUseCase, atLeast(1)).invoke(
            parentHandle = any(),
            nodeSourceType = any(),
            searchParameters = argThat { params ->
                params.query == query &&
                        params.searchCategory == expectedSearchCategory &&
                        params.modificationDate == null &&
                        params.creationDate == null
            },
            isSingleActivityEnabled = any()
        )
    }

    @Test
    fun `test that SearchParameters is constructed correctly with date modified filter`() =
        runTest {
            val query = "test query"
            val dateModifiedFilter = DateFilterOption.Last7Days

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)

            setupTestData(emptyList())

            val underTest = createViewModel()
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            underTest.processAction(
                SearchUiAction.SelectFilter(SearchFilterResult.DateModified(dateModifiedFilter))
            )
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(searchUseCase, atLeast(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.query == query &&
                            params.modificationDate == dateModifiedFilter &&
                            params.creationDate == null
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that SearchParameters is constructed correctly with date added filter`() =
        runTest {
            val query = "test query"
            val dateAddedFilter = DateFilterOption.Today

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)

            setupTestData(emptyList())

            val underTest = createViewModel()
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            underTest.processAction(
                SearchUiAction.SelectFilter(SearchFilterResult.DateAdded(dateAddedFilter))
            )
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(searchUseCase, atLeast(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.query == query &&
                            params.modificationDate == null &&
                            params.creationDate == dateAddedFilter
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that SearchParameters is constructed correctly with all filters`() = runTest {
        val query = "test query"
        val typeFilterOption = TypeFilterOption.Images
        val dateModifiedFilter = DateFilterOption.Last30Days
        val dateAddedFilter = DateFilterOption.ThisYear
        val expectedSearchCategory = SearchCategory.IMAGES

        whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
        whenever(typeFilterToSearchMapper(typeFilterOption, nodeSourceType)).thenReturn(
            expectedSearchCategory
        )

        setupTestData(emptyList())

        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText(query))
        underTest.processAction(SearchUiAction.SelectFilter(SearchFilterResult.Type(typeFilterOption)))
        underTest.processAction(
            SearchUiAction.SelectFilter(SearchFilterResult.DateModified(dateModifiedFilter))
        )
        underTest.processAction(
            SearchUiAction.SelectFilter(SearchFilterResult.DateAdded(dateAddedFilter))
        )
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        verify(searchUseCase, atLeast(1)).invoke(
            parentHandle = any(),
            nodeSourceType = any(),
            searchParameters = argThat { params ->
                params.query == query &&
                        params.searchCategory == expectedSearchCategory &&
                        params.modificationDate == dateModifiedFilter &&
                        params.creationDate == dateAddedFilter
            },
            isSingleActivityEnabled = any()
        )
    }

    @Test
    fun `test that typeFilterToSearchMapper is called with correct nodeSourceType for FAVOURITES`() =
        runTest {
            val query = "test query"
            val favouritesArgs = SearchViewModel.Args(
                parentHandle = parentHandle,
                nodeSourceType = NodeSourceType.FAVOURITES
            )
            val expectedSearchCategory = SearchCategory.FAVOURITES

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(
                expectedSearchCategory
            )

            setupTestData(emptyList())

            val underTest = createViewModel(favouritesArgs)
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(typeFilterToSearchMapper, times(1)).invoke(
                typeFilterOption = null,
                nodeSourceType = NodeSourceType.FAVOURITES
            )

            verify(searchUseCase, times(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.searchCategory == expectedSearchCategory
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that typeFilterToSearchMapper is called with correct nodeSourceType for DOCUMENTS`() =
        runTest {
            val query = "test query"
            val documentsArgs = SearchViewModel.Args(
                parentHandle = parentHandle,
                nodeSourceType = NodeSourceType.DOCUMENTS
            )
            val expectedSearchCategory = SearchCategory.ALL_DOCUMENTS

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(
                expectedSearchCategory
            )

            setupTestData(emptyList())

            val underTest = createViewModel(documentsArgs)
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(typeFilterToSearchMapper, times(1)).invoke(
                typeFilterOption = null,
                nodeSourceType = NodeSourceType.DOCUMENTS
            )

            verify(searchUseCase, times(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.searchCategory == expectedSearchCategory
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that filter update triggers new search with updated parameters`() = runTest {
        val query = "test query"
        val initialTypeFilter = TypeFilterOption.Audio
        val updatedTypeFilter = TypeFilterOption.Video
        val initialSearchCategory = SearchCategory.AUDIO
        val updatedSearchCategory = SearchCategory.VIDEO

        whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
        whenever(typeFilterToSearchMapper(initialTypeFilter, nodeSourceType)).thenReturn(
            initialSearchCategory
        )
        whenever(typeFilterToSearchMapper(updatedTypeFilter, nodeSourceType)).thenReturn(
            updatedSearchCategory
        )

        setupTestData(emptyList())

        val underTest = createViewModel()
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()
        reset(typeFilterToSearchMapper)
        whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
        whenever(typeFilterToSearchMapper(initialTypeFilter, nodeSourceType)).thenReturn(
            initialSearchCategory
        )
        whenever(typeFilterToSearchMapper(updatedTypeFilter, nodeSourceType)).thenReturn(
            updatedSearchCategory
        )

        underTest.processAction(SearchUiAction.UpdateSearchText(query))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        underTest.processAction(
            SearchUiAction.SelectFilter(
                SearchFilterResult.Type(
                    initialTypeFilter
                )
            )
        )
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        underTest.processAction(
            SearchUiAction.SelectFilter(
                SearchFilterResult.Type(
                    updatedTypeFilter
                )
            )
        )
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        verify(typeFilterToSearchMapper, times(1)).invoke(initialTypeFilter, nodeSourceType)
        verify(typeFilterToSearchMapper, times(1)).invoke(updatedTypeFilter, nodeSourceType)

        verify(searchUseCase, atLeast(1)).invoke(
            parentHandle = any(),
            nodeSourceType = any(),
            searchParameters = argThat { params ->
                params.searchCategory == updatedSearchCategory
            },
            isSingleActivityEnabled = any()
        )
    }

    @Test
    fun `test that ItemClicked action in normal mode triggers file open event`() = runTest {
        val underTest = createViewModel()

        val fileNode = mock<TypedFileNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = fileNode,
            isSelected = false
        )

        underTest.processAction(SearchUiAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.openedFileNode).isEqualTo(fileNode)
        }
    }

    @Test
    fun `test that ItemClicked action in normal mode navigates to folder`() = runTest {
        val underTest = createViewModel()

        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = folderNode,
            isSelected = false
        )

        underTest.processAction(SearchUiAction.ItemClicked(nodeUiItem))

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateToFolderEvent).isEqualTo(triggered(folderNode))
        }
    }

    @Test
    fun `test that NavigateToFolderEventConsumed action clear event`() = runTest {
        val underTest = createViewModel()

        val folderNode = mock<TypedFolderNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = folderNode,
            isSelected = false
        )

        underTest.processAction(SearchUiAction.ItemClicked(nodeUiItem))
        advanceUntilIdle()
        underTest.processAction(SearchUiAction.NavigateToFolderEventConsumed)

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateToFolderEvent).isEqualTo(consumed())
        }
    }


    @Test
    fun `test that OpenedFileNodeHandled action clear event`() = runTest {
        val underTest = createViewModel()

        val fileNode = mock<TypedFileNode>()
        val nodeUiItem = NodeUiItem<TypedNode>(
            node = fileNode,
            isSelected = false
        )

        underTest.processAction(SearchUiAction.ItemClicked(nodeUiItem))
        advanceUntilIdle()
        underTest.processAction(SearchUiAction.OpenedFileNodeHandled)

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.openedFileNode).isNull()
        }
    }

    @Test
    fun `test that setCloudSortOrder calls use case and refetches sort order`() = runTest {
        val sortConfiguration =
            NodeSortConfiguration(NodeSortOption.Name, SortDirection.Ascending)
        val expectedSortOrder = SortOrder.ORDER_DEFAULT_ASC

        whenever(nodeSortConfigurationUiMapper(sortConfiguration)).thenReturn(expectedSortOrder)
        whenever(monitorSortCloudOrderUseCase()).thenReturn(flowOf(expectedSortOrder))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.processAction(SearchUiAction.SetSortOrder(sortConfiguration))
        advanceUntilIdle()

        verify(setCloudSortOrderUseCase).invoke(expectedSortOrder)
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from LIST to GRID`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.GRID)
    }

    @Test
    fun `test that ChangeViewTypeClicked action toggles from GRID to LIST`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.GRID))
        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.ChangeViewTypeClicked)
        advanceUntilIdle()

        verify(setViewTypeUseCase).invoke(ViewType.LIST)
    }

    @Test
    fun `test that monitorViewType updates currentViewType in UI state`() = runTest {
        setupTestData(emptyList())
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(ViewType.LIST))
        val underTest = createViewModel()

        underTest.uiState.test {
            val updatedState = awaitItem() // State after monitorViewType flow emits
            assertThat(updatedState.currentViewType).isEqualTo(ViewType.LIST)
        }
    }

    @Test
    fun `test that DeselectAllItems action deselects all items in state`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        underTest.processAction(SearchUiAction.SelectAllItems)
        testScheduler.advanceUntilIdle()

        underTest.processAction(SearchUiAction.DeselectAllItems)
        testScheduler.advanceUntilIdle()

        val updatedState = underTest.uiState.value
        assertThat(updatedState.isInSelectionMode).isFalse()
        assertThat(updatedState.isSelecting).isFalse()
        assertThat(updatedState.items[0].isSelected).isFalse()
        assertThat(updatedState.items[1].isSelected).isFalse()
    }

    @Test
    fun `test that SelectAllItems selects all items when nodes are fully loaded`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        // Wait for initial loading to complete
        testScheduler.advanceUntilIdle()

        // Verify we're in fully loaded state
        val fullyLoadedState = underTest.uiState.value
        assertThat(fullyLoadedState.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)

        underTest.processAction(SearchUiAction.SelectAllItems)
        // Advance the coroutine to let it execute
        testScheduler.advanceUntilIdle()

        val stateAfterSelectAll = underTest.uiState.value
        // Verify that isSelecting is false and all items are selected
        assertThat(stateAfterSelectAll.isSelecting).isFalse()
        assertThat(stateAfterSelectAll.isInSelectionMode).isTrue()
        assertThat(stateAfterSelectAll.items[0].isSelected).isTrue()
        assertThat(stateAfterSelectAll.items[1].isSelected).isTrue()
    }

    @Test
    fun `test that toggleItemSelection removes item from selection when already selected`() =
        runTest {
            val node1 = mock<TypedNode> {
                on { id } doReturn NodeId(1L)
            }

            setupTestData(listOf(node1))
            val underTest = createViewModel()
            underTest.processAction(SearchUiAction.UpdateSearchText("test"))

            underTest.uiState.test {
                awaitItem()
                val loadedState = awaitItem()

                val nodeUiItem1 = loadedState.items[0]
                underTest.processAction(SearchUiAction.ItemLongClicked(nodeUiItem1))
                val stateAfterSelection = awaitItem()

                val updatedNodeUiItem1 = stateAfterSelection.items[0]
                underTest.processAction(SearchUiAction.ItemLongClicked(updatedNodeUiItem1))
                val updatedState = awaitItem()

                assertThat(updatedState.isInSelectionMode).isFalse()
                assertThat(updatedState.items[0].isSelected).isFalse()
            }
        }

    @Test
    fun `test that isInSelectionMode is true when items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(SearchUiAction.ItemLongClicked(nodeUiItem1))
            val state = awaitItem()

            assertThat(state.isInSelectionMode).isTrue()
        }
    }

    @Test
    fun `test that isInSelectionMode is false when no items are selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }

        setupTestData(listOf(node1))
        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            assertThat(loadedState.isInSelectionMode).isFalse()
        }
    }

    @Test
    fun `test that multiple items can be selected`() = runTest {
        val node1 = mock<TypedNode> {
            on { id } doReturn NodeId(1L)
        }
        val node2 = mock<TypedNode> {
            on { id } doReturn NodeId(2L)
        }

        setupTestData(listOf(node1, node2))
        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText("test"))

        underTest.uiState.test {
            awaitItem()
            val loadedState = awaitItem()

            val nodeUiItem1 = loadedState.items[0]
            underTest.processAction(SearchUiAction.ItemLongClicked(nodeUiItem1))
            awaitItem()

            val nodeUiItem2 = loadedState.items[1]
            underTest.processAction(SearchUiAction.ItemLongClicked(nodeUiItem2))
            val updatedState = awaitItem()

            assertThat(updatedState.isInSelectionMode).isTrue()
            assertThat(updatedState.items[0].isSelected).isTrue()
            assertThat(updatedState.items[1].isSelected).isTrue()
        }
    }

    @Test
    fun `test that monitorShowHiddenNodesSettings updates showHiddenNodes`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

            val underTest = createViewModel()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.isHiddenNodeSettingsLoading).isFalse()
                assertThat(finalState.isLoading).isFalse()
                assertThat(finalState.showHiddenNodes).isTrue()
            }
        }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase handles disabled state gracefully`() = runTest {
        setupTestData(emptyList())
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isFalse()
        }
    }

    @Test
    fun `test that monitorHiddenNodesEnabledUseCase handles enabled state correctly`() = runTest {
        setupTestData(emptyList())
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.isHiddenNodesEnabled).isTrue()
        }
    }

    @Test
    fun `test that all hidden nodes properties are updated correctly`() =
        runTest {
            setupTestData(emptyList())
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val finalState = awaitItem()
                assertThat(finalState.showHiddenNodes).isTrue()
                assertThat(finalState.isHiddenNodesEnabled).isTrue()
            }
        }

    @Test
    fun `test that monitorNodeUpdates triggers navigateBack when NodeChanges_Remove is received`() =
        runTest {
            setupTestData(emptyList())
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(parentHandle),
                    nodeSourceType
                )
            ).thenReturn(flowOf(NodeChanges.Remove))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val updatedState = awaitItem()
                assertThat(updatedState.navigateBack).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that monitorNodeUpdates does not trigger performSearch when nodes are loading`() =
        runTest {
            setupTestData(emptyList())

            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(parentHandle),
                    nodeSourceType
                )
            ).thenReturn(flowOf(NodeChanges.Attributes))

            val underTest = createViewModel()
            underTest.processAction(SearchUiAction.UpdateSearchText("test"))

            advanceTimeBy(100)

            verify(searchUseCase, times(0)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that monitorNodeUpdates does not trigger navigateBack for Attributes`() = runTest {
        setupTestData(emptyList())
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(parentHandle),
                nodeSourceType
            )
        ).thenReturn(flowOf(NodeChanges.Attributes))

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val updatedState = awaitItem()
            assertThat(updatedState.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that monitorNodeUpdates handles multiple NodeChanges correctly`() = runTest {
        setupTestData(emptyList())
        val nodeChangesFlow = flowOf(NodeChanges.Attributes, NodeChanges.Remove)
        whenever(
            monitorNodeUpdatesByIdUseCase(
                NodeId(parentHandle),
                nodeSourceType
            )
        ).thenReturn(nodeChangesFlow)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val finalState = awaitItem()
            assertThat(finalState.navigateBack).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that NavigateBackEventConsumed action consumes the navigate back event`() = runTest {
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(NodeId(parentHandle))).thenReturn(flowOf(NodeChanges.Remove))

        val underTest = createViewModel()

        underTest.uiState.test {
            val stateAfterRemove = awaitItem() // State after Remove triggers navigateBack
            assertThat(stateAfterRemove.navigateBack).isEqualTo(triggered)

            underTest.processAction(SearchUiAction.NavigateBackEventConsumed)
            val stateAfterConsume = awaitItem() // State after consuming the event
            assertThat(stateAfterConsume.navigateBack).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that SearchParameters includes description and tag when nodeSourceType is not RUBBISH_BIN`() =
        runTest {
            val query = "#test"
            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file.txt")
            }
            setupTestData(listOf(typedFileNode))

            val underTest = createViewModel()
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(searchUseCase, atLeast(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.description == query && params.tag == "test"
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that SearchParameters includes description but tag is null when nodeSourceType is RUBBISH_BIN`() =
        runTest {
            val query = "#test"
            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
            val rubbishBinArgs = SearchViewModel.Args(
                parentHandle = parentHandle,
                nodeSourceType = NodeSourceType.RUBBISH_BIN
            )
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(NodeId(123L))
                on { name }.thenReturn("file.txt")
            }
            setupTestData(listOf(typedFileNode))
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(parentHandle),
                    NodeSourceType.RUBBISH_BIN
                )
            ).thenReturn(flowOf())

            val underTest = createViewModel(rubbishBinArgs)
            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(searchUseCase, atLeast(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.description == query && params.tag == null
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that SearchParameters tag removes hash prefix correctly`() = runTest {
        val query = "test"
        whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file.txt")
        }
        setupTestData(listOf(typedFileNode))

        val underTest = createViewModel()
        underTest.processAction(SearchUiAction.UpdateSearchText(query))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
        advanceUntilIdle()

        verify(searchUseCase, atLeast(1)).invoke(
            parentHandle = any(),
            nodeSourceType = any(),
            searchParameters = argThat { params ->
                params.description == query && params.tag == query
            },
            isSingleActivityEnabled = any()
        )
    }

    @Test
    fun `test that nodeSourceTypeToSearchTargetMapper is called with correct nodeSourceType and SearchTarget is used in SearchParameters`() =
        runTest {
            val query = "test query"
            val incomingSharesArgs = SearchViewModel.Args(
                parentHandle = parentHandle,
                nodeSourceType = NodeSourceType.INCOMING_SHARES
            )
            val expectedSearchTarget = SearchTarget.INCOMING_SHARE

            whenever(typeFilterToSearchMapper(anyOrNull(), any())).thenReturn(SearchCategory.ALL)
            setupTestData(emptyList())
            whenever(
                monitorNodeUpdatesByIdUseCase(
                    NodeId(parentHandle),
                    NodeSourceType.INCOMING_SHARES
                )
            ).thenReturn(flowOf())
            whenever(nodeSourceTypeToSearchTargetMapper(NodeSourceType.INCOMING_SHARES)).thenReturn(
                expectedSearchTarget
            )

            val underTest = createViewModel(incomingSharesArgs)
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()
            reset(nodeSourceTypeToSearchTargetMapper)
            whenever(nodeSourceTypeToSearchTargetMapper(NodeSourceType.INCOMING_SHARES)).thenReturn(
                expectedSearchTarget
            )

            underTest.processAction(SearchUiAction.UpdateSearchText(query))
            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            advanceUntilIdle()

            verify(nodeSourceTypeToSearchTargetMapper, atLeast(1)).invoke(
                nodeSourceType = NodeSourceType.INCOMING_SHARES
            )

            verify(searchUseCase, atLeast(1)).invoke(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = argThat { params ->
                    params.searchTarget == expectedSearchTarget
                },
                isSingleActivityEnabled = any()
            )
        }

    @Test
    fun `test that updateSearchPlaceholder sets placeholder when parentHandle is -1L`() = runTest {
        val args = SearchViewModel.Args(
            parentHandle = -1L,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE
        )
        val expectedPlaceholder =
            LocalizedText.StringRes(sharedR.string.search_placeholder_cloud_drive)
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())
        whenever(
            searchPlaceholderMapper(
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                nodeName = null
            )
        ).thenReturn(expectedPlaceholder)

        val underTest = createViewModel(args)
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.placeholderText).isEqualTo(expectedPlaceholder)
        }
    }

    @Test
    fun `test that updateSearchPlaceholder sets placeholder when node name is available`() =
        runTest {
            val nodeName = "My Folder"
            val nodeInfo = NodeInfo(name = nodeName, isNodeKeyDecrypted = true)
            val expectedPlaceholder = LocalizedText.StringRes(
                resId = sharedR.string.search_placeholder_folder,
                formatArgs = listOf(nodeName)
            )
            setupTestData(emptyList())
            whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())
            whenever(getNodeInfoByIdUseCase(NodeId(parentHandle))).thenReturn(nodeInfo)
            whenever(
                searchPlaceholderMapper(
                    nodeSourceType = nodeSourceType,
                    nodeName = nodeName
                )
            ).thenReturn(expectedPlaceholder)

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.placeholderText).isEqualTo(expectedPlaceholder)
            }
        }

    @Test
    fun `test that updateSearchPlaceholder sets placeholder when node info is null`() = runTest {
        val expectedPlaceholder =
            LocalizedText.StringRes(sharedR.string.search_bar_placeholder_text)
        setupTestData(emptyList())
        whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())
        whenever(getNodeInfoByIdUseCase(NodeId(parentHandle))).thenReturn(null)
        whenever(
            searchPlaceholderMapper(
                nodeSourceType = nodeSourceType,
                nodeName = null
            )
        ).thenReturn(expectedPlaceholder)

        val underTest = createViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.placeholderText).isEqualTo(expectedPlaceholder)
        }
    }

    @Test
    fun `test that updateSearchPlaceholder falls back to default when mapper throws exception`() =
        runTest {
            val nodeInfo = NodeInfo(name = "My Folder", isNodeKeyDecrypted = true)
            val expectedPlaceholder =
                LocalizedText.StringRes(sharedR.string.search_bar_placeholder_text)
            setupTestData(emptyList())
            whenever(monitorNodeUpdatesByIdUseCase(any(), any())).thenReturn(flowOf())
            whenever(getNodeInfoByIdUseCase(NodeId(parentHandle))).thenReturn(nodeInfo)
            whenever(
                searchPlaceholderMapper(
                    nodeSourceType = nodeSourceType,
                    nodeName = nodeInfo.name
                )
            ).thenThrow(RuntimeException("Mapper error"))

            val underTest = createViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.placeholderText).isEqualTo(expectedPlaceholder)
            }
        }

}