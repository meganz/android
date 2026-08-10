package mega.privacy.android.feature.cloudexplorer.presentation.search

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AsSearchStateTest {

    @Test
    fun `test that asSearchState projects the search items onto the items`() {
        val searchItems = listOf(previewFolderNodeUiItem(1L))
        val state = nodeExplorerDataState(
            items = emptyList(),
            searchItems = searchItems,
            searchedQuery = QUERY,
            searchLoadingState = NodesLoadingState.FullyLoaded,
        )

        val actual = state.asSearchState(QUERY) as NodeExplorerUiState.Data
        assertThat(actual.items).isEqualTo(searchItems)
    }

    @Test
    fun `test that asSearchState uses the search loading state when the query matches the searched query`() {
        val state = nodeExplorerDataState(
            searchedQuery = QUERY,
            searchLoadingState = NodesLoadingState.FullyLoaded,
        )

        val actual = state.asSearchState(QUERY) as NodeExplorerUiState.Data
        assertThat(actual.nodesLoadingState).isEqualTo(NodesLoadingState.FullyLoaded)
    }

    @Test
    fun `test that asSearchState stays loading when the query does not match the searched query`() {
        val state = nodeExplorerDataState(
            searchedQuery = QUERY,
            searchLoadingState = NodesLoadingState.FullyLoaded,
        )

        val actual = state.asSearchState("different") as NodeExplorerUiState.Data
        assertThat(actual.nodesLoadingState).isEqualTo(NodesLoadingState.Loading)
    }

    private companion object {
        const val QUERY = "report"
    }
}
