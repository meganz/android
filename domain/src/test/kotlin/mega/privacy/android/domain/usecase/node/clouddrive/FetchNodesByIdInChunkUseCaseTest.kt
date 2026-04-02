package mega.privacy.android.domain.usecase.node.clouddrive

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchNodesByIdInChunkUseCaseTest {

    private lateinit var underTest: FetchNodesByIdInChunkUseCase

    private val nodeRepository = mock<NodeRepository>()
    private val getFolderTypeDataUseCase = mock<GetFolderTypeDataUseCase>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val containsMediaItemUseCase = mock<ContainsMediaItemUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = FetchNodesByIdInChunkUseCase(
            nodeRepository = nodeRepository,
            getFolderTypeDataUseCase = getFolderTypeDataUseCase,
            getCloudSortOrder = getCloudSortOrder,
            containsMediaItemUseCase = containsMediaItemUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository,
            getFolderTypeDataUseCase,
            getCloudSortOrder,
            containsMediaItemUseCase,
        )
    }

    @Test
    fun `test that invoke emits PartiallyLoaded state when repository emits hasMore true`() =
        runTest {
            val nodes = listOf(mock<TypedNode>())
            whenever(getCloudSortOrder()).thenReturn(mock())
            whenever(getFolderTypeDataUseCase()).thenReturn(mock())
            whenever(containsMediaItemUseCase(any())).thenReturn(false)
            whenever(
                nodeRepository.getTypedNodesByIdInChunks(
                    nodeId = any(),
                    order = any(),
                    initialBatchSize = any(),
                    folderTypeData = any(),
                )
            ).thenReturn(flowOf(Pair(nodes, true)))

            underTest(NodeId(1L)).test {
                val result = awaitItem()
                assertThat(result.loadingState).isEqualTo(NodesLoadingState.PartiallyLoaded)
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke emits FullyLoaded state when repository emits hasMore false`() =
        runTest {
            val nodes = listOf(mock<TypedNode>())
            whenever(getCloudSortOrder()).thenReturn(mock())
            whenever(getFolderTypeDataUseCase()).thenReturn(mock())
            whenever(containsMediaItemUseCase(any())).thenReturn(false)
            whenever(
                nodeRepository.getTypedNodesByIdInChunks(
                    nodeId = any(),
                    order = any(),
                    initialBatchSize = any(),
                    folderTypeData = any(),
                )
            ).thenReturn(flowOf(Pair(nodes, false)))

            underTest(NodeId(1L)).test {
                val result = awaitItem()
                assertThat(result.loadingState).isEqualTo(NodesLoadingState.FullyLoaded)
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke emits correct hasMediaItems from containsMediaItemUseCase`() =
        runTest {
            val nodes = listOf(mock<TypedNode>())
            whenever(getCloudSortOrder()).thenReturn(mock())
            whenever(getFolderTypeDataUseCase()).thenReturn(mock())
            whenever(containsMediaItemUseCase(nodes)).thenReturn(true)
            whenever(
                nodeRepository.getTypedNodesByIdInChunks(
                    nodeId = any(),
                    order = any(),
                    initialBatchSize = any(),
                    folderTypeData = any(),
                )
            ).thenReturn(flowOf(Pair(nodes, false)))

            underTest(NodeId(1L)).test {
                val result = awaitItem()
                assertThat(result.hasMediaItems).isTrue()
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke emits typed nodes from repository`() = runTest {
        val nodes = listOf(mock<TypedNode>(), mock<TypedNode>())
        whenever(getCloudSortOrder()).thenReturn(mock())
        whenever(getFolderTypeDataUseCase()).thenReturn(mock())
        whenever(containsMediaItemUseCase(any())).thenReturn(false)
        whenever(
            nodeRepository.getTypedNodesByIdInChunks(
                nodeId = any(),
                order = any(),
                initialBatchSize = any(),
                folderTypeData = any(),
            )
        ).thenReturn(flowOf(Pair(nodes, false)))

        underTest(NodeId(1L)).test {
            val result = awaitItem()
            assertThat(result.typedNodes).isEqualTo(nodes)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke passes initialBatchSize to repository`() = runTest {
        val batchSize = 200
        whenever(getCloudSortOrder()).thenReturn(mock())
        whenever(getFolderTypeDataUseCase()).thenReturn(mock())
        whenever(containsMediaItemUseCase(any())).thenReturn(false)
        whenever(
            nodeRepository.getTypedNodesByIdInChunks(
                nodeId = any(),
                order = any(),
                initialBatchSize = any(),
                folderTypeData = any(),
            )
        ).thenReturn(flowOf(Pair(emptyList(), false)))

        underTest(nodeId = NodeId(1L), initialBatchSize = batchSize).test {
            awaitItem()
            awaitComplete()
        }

        verify(nodeRepository).getTypedNodesByIdInChunks(
            nodeId = any(),
            order = anyOrNull(),
            initialBatchSize = eq(batchSize),
            folderTypeData = anyOrNull(),
        )
    }

    @Test
    fun `test that invoke passes sort order from getCloudSortOrder to repository`() = runTest {
        val sortOrder = SortOrder.ORDER_DEFAULT_ASC
        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(getFolderTypeDataUseCase()).thenReturn(mock())
        whenever(containsMediaItemUseCase(any())).thenReturn(false)
        whenever(
            nodeRepository.getTypedNodesByIdInChunks(
                nodeId = any(),
                order = any(),
                initialBatchSize = any(),
                folderTypeData = any(),
            )
        ).thenReturn(flowOf(Pair(emptyList(), false)))

        underTest(NodeId(1L)).test {
            awaitItem()
            awaitComplete()
        }

        verify(nodeRepository).getTypedNodesByIdInChunks(
            nodeId = any(),
            order = eq(sortOrder),
            initialBatchSize = any(),
            folderTypeData = anyOrNull(),
        )
    }

    @Test
    fun `test that invoke passes folder type data from getFolderTypeDataUseCase to repository`() =
        runTest {
            val folderTypeData = mock<mega.privacy.android.domain.entity.FolderTypeData>()
            whenever(getCloudSortOrder()).thenReturn(mock())
            whenever(getFolderTypeDataUseCase()).thenReturn(folderTypeData)
            whenever(containsMediaItemUseCase(any())).thenReturn(false)
            whenever(
                nodeRepository.getTypedNodesByIdInChunks(
                    nodeId = any(),
                    order = any(),
                    initialBatchSize = any(),
                    folderTypeData = any(),
                )
            ).thenReturn(flowOf(Pair(emptyList(), false)))

            underTest(NodeId(1L)).test {
                awaitItem()
                awaitComplete()
            }

            verify(nodeRepository).getTypedNodesByIdInChunks(
                nodeId = any(),
                order = anyOrNull(),
                initialBatchSize = any(),
                folderTypeData = eq(folderTypeData),
            )
        }

    @Test
    fun `test that invoke emits Failed state when repository throws exception`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(mock())
        whenever(getFolderTypeDataUseCase()).thenReturn(mock())
        whenever(
            nodeRepository.getTypedNodesByIdInChunks(
                nodeId = any(),
                order = any(),
                initialBatchSize = any(),
                folderTypeData = any(),
            )
        ).thenReturn(flow { throw RuntimeException("error") })

        underTest(NodeId(1L)).test {
            val result = awaitItem()
            assertThat(result.loadingState).isEqualTo(NodesLoadingState.Failed)
            assertThat(result.hasMediaItems).isFalse()
            assertThat(result.typedNodes).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke uses default initialBatchSize of 500`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(mock())
        whenever(getFolderTypeDataUseCase()).thenReturn(mock())
        whenever(containsMediaItemUseCase(any())).thenReturn(false)
        whenever(
            nodeRepository.getTypedNodesByIdInChunks(
                nodeId = any(),
                order = any(),
                initialBatchSize = any(),
                folderTypeData = any(),
            )
        ).thenReturn(flowOf(Pair(emptyList(), false)))

        underTest(NodeId(1L)).test {
            awaitItem()
            awaitComplete()
        }

        verify(nodeRepository).getTypedNodesByIdInChunks(
            nodeId = any(),
            order = anyOrNull(),
            initialBatchSize = eq(500),
            folderTypeData = anyOrNull(),
        )
    }
}
