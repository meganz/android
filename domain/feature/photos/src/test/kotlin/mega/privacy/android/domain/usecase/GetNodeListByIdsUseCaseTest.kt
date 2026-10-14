package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetNodeListByIdsUseCaseTest {
    private lateinit var underTest: GetNodeListByIdsUseCase
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        underTest = GetNodeListByIdsUseCase(getNodeByIdUseCase, ioDispatcher)
    }

    @Test
    fun `test that use case returns list of typed nodes when all ids are valid`() = runTest {
        val ids = listOf(1L, 2L, 3L).map { NodeId(it) }
        val node1 = mock<TypedNode>()
        val node2 = mock<TypedNode>()
        val node3 = mock<TypedNode>()

        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node1)
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(node2)
        whenever(getNodeByIdUseCase(NodeId(3L))).thenReturn(node3)

        val result = underTest(ids)

        assertThat(result).containsExactly(node1, node2, node3).inOrder()
    }

    @Test
    fun `test that use case filters out null nodes when some ids are invalid`() = runTest {
        val ids = listOf(1L, 2L, 3L).map { NodeId(it) }
        val node1 = mock<TypedNode>()
        val node3 = mock<TypedNode>()

        whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(node1)
        whenever(getNodeByIdUseCase(NodeId(2L))).thenReturn(null)
        whenever(getNodeByIdUseCase(NodeId(3L))).thenReturn(node3)

        val result = underTest(ids)

        assertThat(result).containsExactly(node1, node3).inOrder()
    }
}

