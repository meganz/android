package mega.privacy.android.feature.clouddrive.presentation.search

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiAction
import mega.privacy.android.navigation.destination.SearchNavKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
    private val navKey = SearchNavKey(
        parentHandle = 123L,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE
    )

    @AfterEach
    fun tearDown() {
        reset(searchUseCase, cancelCancelTokenUseCase, nodeUiItemMapper)
    }

    private fun createViewModel() = SearchViewModel(
        navKey = navKey,
        searchUseCase = searchUseCase,
        cancelCancelTokenUseCase = cancelCancelTokenUseCase,
        nodeUiItemMapper = nodeUiItemMapper,
    )

    private suspend fun stubNodeUiItemMapper(result: List<NodeUiItem<TypedNode>>) {
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

        underTest.processAction(SearchUiAction.UpdateSearchText("test"))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

        underTest.uiState.test {
            val stateWithResults = awaitItem()
            assertThat(stateWithResults.items).hasSize(1)

            underTest.processAction(SearchUiAction.UpdateSearchText(""))
            awaitItem() // searchText update

            advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)
            val stateAfterClear = awaitItem()
            assertThat(stateAfterClear.items).isEmpty()
            assertThat(stateAfterClear.searchedQuery).isEmpty()
            assertThat(stateAfterClear.nodesLoadingState).isEqualTo(NodesLoadingState.Idle)
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
    fun `test that search result contains both files and folders`() = runTest {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(123L))
            on { name }.thenReturn("file.txt")
        }
        val typedFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(456L))
            on { name }.thenReturn("folder")
        }
        val fileNodeUiItem = mock<NodeUiItem<TypedNode>> {
            on { node }.thenReturn(typedFileNode)
        }
        val folderNodeUiItem = mock<NodeUiItem<TypedNode>> {
            on { node }.thenReturn(typedFolderNode)
        }
        whenever(
            searchUseCase(
                parentHandle = any(),
                nodeSourceType = any(),
                searchParameters = any(),
                isSingleActivityEnabled = any(),
            )
        ).thenReturn(listOf(typedFileNode, typedFolderNode))
        stubNodeUiItemMapper(listOf(fileNodeUiItem, folderNodeUiItem))

        val underTest = createViewModel()

        underTest.processAction(SearchUiAction.UpdateSearchText("test"))
        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.items).hasSize(2)
        }
    }

    @Test
    fun `test that searchUseCase is called only once when debounce time is reached`() = runTest {
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

        underTest.processAction(SearchUiAction.UpdateSearchText("a"))
        advanceTimeBy(100)
        underTest.processAction(SearchUiAction.UpdateSearchText("ab"))
        advanceTimeBy(100)
        underTest.processAction(SearchUiAction.UpdateSearchText("abc"))

        advanceTimeBy(SearchViewModel.SEARCH_DEBOUNCE_MS + 100)

        verify(searchUseCase, times(1)).invoke(
            parentHandle = any(),
            nodeSourceType = any(),
            searchParameters = any(),
            isSingleActivityEnabled = any(),
        )
    }
}
