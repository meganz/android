package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCloudDriveNodesUseCaseTest {
    private lateinit var underTest: MonitorCloudDriveNodesUseCase

    private val photosRepository = mock<PhotosRepository>()
    private val nodeRepository = mock<NodeRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorCloudDriveNodesUseCase(
            photosRepository,
            nodeRepository,
            getCloudSortOrder,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(photosRepository, nodeRepository, getCloudSortOrder)
    }

    @Test
    fun `test that initial nodes are emitted`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_NONE
        val initialNodes = listOf(
            mock<ImageNode>(),
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(photosRepository.fetchImageNodes(parentId, sortOrder)).thenReturn(initialNodes)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }

        verify(photosRepository).fetchImageNodes(parentId, sortOrder)
    }

    @Test
    fun `test that invoke should emit updated nodes when node update occurs`() = runTest {
        val tempParentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_NONE
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(11L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
                on { parentId }.thenReturn(tempParentId)
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(photosRepository.fetchImageNodes(tempParentId, sortOrder)).thenReturn(initialNodes)

        val imageNode1 = mock<ImageNode> {
            on { id }.thenReturn(NodeId(2L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            on { parentId }.thenReturn(tempParentId)
        }

        val imageNode2 = mock<ImageNode> {
            on { id }.thenReturn(NodeId(3L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            on { parentId }.thenReturn(tempParentId)
        }

        val update = mapOf<Node, List<NodeChanges>>(
            imageNode1 to listOf(NodeChanges.Parent),
            imageNode2 to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(
            photosRepository.fetchImageNode(
                nodeId = NodeId(2L),
                filterSvg = false
            )
        ).thenReturn(imageNode1)
        whenever(
            photosRepository.fetchImageNode(
                nodeId = NodeId(3L),
                filterSvg = false
            )
        ).thenReturn(imageNode2)
        val nodeUpdate = NodeUpdate(update)

        underTest(tempParentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            assertThat(awaitItem().size).isEqualTo(3)
            cancelAndIgnoreRemainingEvents()
        }
    }
}