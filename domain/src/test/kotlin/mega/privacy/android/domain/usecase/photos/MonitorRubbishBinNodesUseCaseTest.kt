package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
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
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorRubbishBinNodesUseCaseTest {
    private lateinit var underTest: MonitorRubbishBinNodesUseCase

    private val photosRepository = mock<PhotosRepository>()
    private val nodeRepository = mock<NodeRepository>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorRubbishBinNodesUseCase(
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
    fun `test that initial nodes are emitted from rubbish bin`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            },
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(3L))
                on { type }.thenReturn(VideoFileTypeInfo("", "", 10.seconds))
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(initialNodes)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }

        verify(getCloudSortOrder)()
        verify(photosRepository).fetchImageNodes(parentId, sortOrder, includeRubbishBin = true)
    }

    @Test
    fun `test that empty list is returned when no nodes are found in rubbish bin`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(emptyList())
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())

        underTest(parentId).test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke should emit updated nodes when node update occurs`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(initialNodes)

        val newImageNode = mock<ImageNode> {
            on { id }.thenReturn(NodeId(3L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val fileNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { this.parentId }.thenReturn(parentId)
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            fileNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(
            photosRepository.fetchImageNode(
                nodeId = NodeId(3L),
                filterSvg = false,
                includeRubbishBin = true
            )
        ).thenReturn(newImageNode)
        val nodeUpdate = NodeUpdate(update)

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            val updatedNodes = awaitItem()
            assertThat(updatedNodes.size).isEqualTo(2)
            assertThat(updatedNodes).contains(newImageNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that non-media nodes are filtered out`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(initialNodes)

        val nonMediaNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { this.parentId }.thenReturn(parentId)
            on { type }.thenReturn(UnknownFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            nonMediaNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        val nodeUpdate = NodeUpdate(update)

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nodes with different parent ID are filtered out`() = runTest {
        val parentId = NodeId(1L)
        val differentParentId = NodeId(2L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(3L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(initialNodes)

        val imageNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(4L))
            on { this.parentId }.thenReturn(differentParentId)
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            imageNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        val nodeUpdate = NodeUpdate(update)

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that video nodes are included`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(initialNodes)

        val videoNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { this.parentId }.thenReturn(parentId)
            on { type }.thenReturn(VideoFileTypeInfo("", "", 10.seconds))
        }

        val videoImageNode = mock<ImageNode> {
            on { id }.thenReturn(NodeId(3L))
            on { type }.thenReturn(VideoFileTypeInfo("", "", 10.seconds))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            videoNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(
            photosRepository.fetchImageNode(
                nodeId = NodeId(3L),
                filterSvg = false,
                includeRubbishBin = true
            )
        ).thenReturn(videoImageNode)
        val nodeUpdate = NodeUpdate(update)

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            val updatedNodes = awaitItem()
            assertThat(updatedNodes.size).isEqualTo(2)
            assertThat(updatedNodes).contains(videoImageNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that removed nodes are filtered out`() = runTest {
        val parentId = NodeId(1L)
        val sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            },
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(3L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(getCloudSortOrder()).thenReturn(sortOrder)
        whenever(
            photosRepository.fetchImageNodes(
                parentId,
                sortOrder,
                includeRubbishBin = true
            )
        ).thenReturn(initialNodes)

        val fileNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { this.parentId }.thenReturn(parentId)
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            fileNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(
            photosRepository.fetchImageNode(
                nodeId = NodeId(3L),
                filterSvg = false,
                includeRubbishBin = true
            )
        ).thenReturn(null)
        val nodeUpdate = NodeUpdate(update)

        underTest(parentId).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            val updatedNodes = awaitItem()
            assertThat(updatedNodes.size).isEqualTo(1)
            assertThat(updatedNodes).contains(initialNodes[0])
            cancelAndIgnoreRemainingEvents()
        }
    }
}