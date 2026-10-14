package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.MonitorImageNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultImageNodeFetcherTest {
    private lateinit var underTest: DefaultImageNodeFetcher

    private val monitorImageNodesUseCase = mock<MonitorImageNodesUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DefaultImageNodeFetcher(
            monitorImageNodesUseCase = monitorImageNodesUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorImageNodesUseCase)
    }

    @Test
    fun `test that monitorImageNodes sorts by modification time when preserve order is not set`() =
        runTest {
            val older = createImageNode(id = 1L, modificationTime = 100L)
            val newer = createImageNode(id = 2L, modificationTime = 200L)
            stubImageNodes(nodeIds = listOf(1L, 2L), nodes = listOf(older, newer))

            underTest.monitorImageNodes(createBundle(listOf(1L, 2L), isOrderPreserved = false))
                .test {
                    assertThat(awaitItem()).containsExactly(newer, older).inOrder()
                    awaitComplete()
                }
        }

    @Test
    fun `test that monitorImageNodes keeps the order of the node ids when preserve order is set`() =
        runTest {
            val first = createImageNode(id = 30L, modificationTime = 100L)
            val second = createImageNode(id = 10L, modificationTime = 300L)
            val third = createImageNode(id = 20L, modificationTime = 200L)
            val nodeIds = listOf(30L, 10L, 20L)
            stubImageNodes(nodeIds = nodeIds, nodes = listOf(third, second, first))

            underTest.monitorImageNodes(createBundle(nodeIds, isOrderPreserved = true)).test {
                assertThat(awaitItem()).containsExactly(first, second, third).inOrder()
                awaitComplete()
            }
        }

    private fun stubImageNodes(nodeIds: List<Long>, nodes: List<ImageNode>) {
        whenever(monitorImageNodesUseCase(nodeIds.map { NodeId(it) })).thenReturn(flowOf(nodes))
    }

    private fun createBundle(nodeIds: List<Long>, isOrderPreserved: Boolean) = mock<Bundle> {
        on { getLongArray(DefaultImageNodeFetcher.NODE_IDS) } doReturn nodeIds.toLongArray()
        on {
            getBoolean(DefaultImageNodeFetcher.PRESERVE_ORDER, false)
        } doReturn isOrderPreserved
    }

    private fun createImageNode(id: Long, modificationTime: Long) = mock<ImageNode> {
        on { this.id } doReturn NodeId(id)
        on { this.modificationTime } doReturn modificationTime
    }
}
