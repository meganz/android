package mega.privacy.android.feature.clouddrive.presentation.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.TypeFilterToSearchMapper
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterResult
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeast
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
    private val nodeSourceType = NodeSourceType.CLOUD_DRIVE
    private val parentHandle = 123L
    private val args = SearchViewModel.Args(
        parentHandle = parentHandle,
        nodeSourceType = nodeSourceType
    )

    @AfterEach
    fun tearDown() {
        reset(searchUseCase, cancelCancelTokenUseCase, nodeUiItemMapper)
    }

    private fun createViewModel(
        args: SearchViewModel.Args = this.args,
    ) = SearchViewModel(
        args = args,
        searchUseCase = searchUseCase,
        cancelCancelTokenUseCase = cancelCancelTokenUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
        typeFilterToSearchMapper = typeFilterToSearchMapper
    )

    private fun stubNodeUiItemMapper(result: List<NodeUiItem<TypedNode>>) {
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
            ).thenReturn(result)
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
        val nodeUiItem = mock<NodeUiItem<TypedNode>>()
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        stubNodeUiItemMapper(listOf(nodeUiItem))

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
        val nodeUiItem = mock<NodeUiItem<TypedNode>>()
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        stubNodeUiItemMapper(listOf(nodeUiItem))

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
        stubNodeUiItemMapper(listOf(nodeUiItem))

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
        stubNodeUiItemMapper(emptyList())

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
        val nodeUiItem = mock<NodeUiItem<TypedNode>>()
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode))
        stubNodeUiItemMapper(listOf(nodeUiItem))

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
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
            stubNodeUiItemMapper(emptyList())

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
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
            stubNodeUiItemMapper(emptyList())

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
        whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
        stubNodeUiItemMapper(emptyList())

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
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
            stubNodeUiItemMapper(emptyList())

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
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
            stubNodeUiItemMapper(emptyList())

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
        whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
        stubNodeUiItemMapper(emptyList())

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
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
            stubNodeUiItemMapper(emptyList())

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
            whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
            stubNodeUiItemMapper(emptyList())

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
        whenever(searchUseCase(any(), any(), any(), any())).thenReturn(emptyList())
        stubNodeUiItemMapper(emptyList())

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

}