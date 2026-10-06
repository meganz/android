package mega.privacy.android.shared.nodes.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PagedNodesTest {

    @ParameterizedTest(name = "{0} reads as {1}")
    @MethodSource("loadStates")
    fun `test that nodesLoadingState maps every paged load state`(
        loadState: PagedLoadState,
        expected: NodesLoadingState,
    ) {
        val paged = PagedNodes<TypedNode>(loadState = loadState)

        assertThat(paged.nodesLoadingState).isEqualTo(expected)
    }

    private fun loadStates() = listOf(
        Arguments.of(PagedLoadState.FirstPage, NodesLoadingState.Loading),
        Arguments.of(PagedLoadState.Idle, NodesLoadingState.FullyLoaded),
        Arguments.of(PagedLoadState.LoadingMore, NodesLoadingState.PartiallyLoaded),
        Arguments.of(PagedLoadState.FirstPageFailed, NodesLoadingState.Failed),
        Arguments.of(PagedLoadState.PageFailed, NodesLoadingState.FullyLoaded),
    )
}
