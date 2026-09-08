package mega.privacy.android.domain.usecase.recentactions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.MonitorImageNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorRecentActionBucketImageNodesUseCaseTest {
    private lateinit var underTest: MonitorRecentActionBucketImageNodesUseCase

    private val getRecentActionBucketByIdUseCase = mock<GetRecentActionBucketByIdUseCase>()
    private val monitorImageNodesUseCase = mock<MonitorImageNodesUseCase>()

    private val bucketId = "bucket-id"
    private val anchorNodeId = NodeId(1L)

    @BeforeAll
    fun setUp() {
        underTest = MonitorRecentActionBucketImageNodesUseCase(
            getRecentActionBucketByIdUseCase = getRecentActionBucketByIdUseCase,
            monitorImageNodesUseCase = monitorImageNodesUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getRecentActionBucketByIdUseCase, monitorImageNodesUseCase)
    }

    @Test
    fun `test that invoke emits image nodes of the bucket nodes`() = runTest {
        val nodeIds = listOf(anchorNodeId, NodeId(2L))
        val bucket = stubBucket(nodeIds)
        val imageNodes = listOf(mock<ImageNode>(), mock<ImageNode>())
        whenever(getRecentActionBucketByIdUseCase(id = bucketId, excludeSensitives = false))
            .thenReturn(bucket)
        whenever(monitorImageNodesUseCase(nodeIds)).thenReturn(flowOf(imageNodes))

        underTest(bucketId = bucketId, anchorNodeId = anchorNodeId).test {
            assertThat(awaitItem()).isEqualTo(imageNodes)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke does not exclude sensitive nodes from the bucket lookup`() = runTest {
        val bucket = stubBucket(emptyList())
        whenever(getRecentActionBucketByIdUseCase(id = bucketId, excludeSensitives = false))
            .thenReturn(bucket)
        whenever(monitorImageNodesUseCase(emptyList())).thenReturn(flowOf(emptyList()))

        underTest(bucketId = bucketId, anchorNodeId = anchorNodeId).test {
            awaitItem()
            awaitComplete()
        }

        verify(getRecentActionBucketByIdUseCase)(id = bucketId, excludeSensitives = false)
    }

    @Test
    fun `test that invoke monitors only the anchor node when bucket is not found`() = runTest {
        val anchorImageNode = mock<ImageNode>()
        whenever(getRecentActionBucketByIdUseCase(id = bucketId, excludeSensitives = false))
            .thenReturn(null)
        whenever(monitorImageNodesUseCase(listOf(anchorNodeId)))
            .thenReturn(flowOf(listOf(anchorImageNode)))

        underTest(bucketId = bucketId, anchorNodeId = anchorNodeId).test {
            assertThat(awaitItem()).containsExactly(anchorImageNode)
            awaitComplete()
        }
    }

    private fun stubBucket(nodeIds: List<NodeId>): RecentActionBucket {
        val nodes = nodeIds.map { nodeId ->
            mock<TypedFileNode> { on { id }.thenReturn(nodeId) }
        }
        return mock<RecentActionBucket> { on { this.nodes }.thenReturn(nodes) }
    }
}
