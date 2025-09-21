package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
class MonitorImageNodesUseCaseTest {
    private lateinit var underTest: MonitorImageNodesUseCase

    private val photosRepository = mock<PhotosRepository>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorImageNodesUseCase(
            photosRepository,
            nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(photosRepository, nodeRepository)
    }

    @Test
    fun `test that initial nodes are emitted`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L))
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            },
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(initialNodes[0])
        whenever(photosRepository.fetchImageNode(NodeId(2L), false)).thenReturn(initialNodes[1])
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }

        verify(photosRepository).fetchImageNode(NodeId(1L), false)
        verify(photosRepository).fetchImageNode(NodeId(2L), false)
    }

    @Test
    fun `test that empty list is returned when no nodes are found`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L))

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(null)
        whenever(photosRepository.fetchImageNode(NodeId(2L), false)).thenReturn(null)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf())

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke should emit updated nodes when node update occurs`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L))
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(initialNodes[0])
        whenever(photosRepository.fetchImageNode(NodeId(2L), false)).thenReturn(null)

        val updatedImageNode = mock<ImageNode> {
            on { id }.thenReturn(NodeId(1L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val fileNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(1L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            fileNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(photosRepository.fetchImageNode(nodeId = NodeId(1L))).thenReturn(updatedImageNode)
        val nodeUpdate = NodeUpdate(update)

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            val updatedNodes = awaitItem()
            assertThat(updatedNodes.size).isEqualTo(1)
            assertThat(updatedNodes).contains(updatedImageNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that non-media nodes are filtered out`() = runTest {
        val nodeIds = listOf(NodeId(1L))
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(initialNodes[0])

        val nonMediaNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { type }.thenReturn(UnknownFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            nonMediaNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        val nodeUpdate = NodeUpdate(update)

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that video nodes are included`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L))
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(initialNodes[0])
        whenever(
            photosRepository.fetchImageNode(
                NodeId(2L),
                false
            )
        ).thenReturn(null)

        val videoNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { type }.thenReturn(VideoFileTypeInfo("", "", 10.seconds))
        }

        val videoImageNode = mock<ImageNode> {
            on { id }.thenReturn(NodeId(2L))
            on { type }.thenReturn(VideoFileTypeInfo("", "", 10.seconds))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            videoNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(photosRepository.fetchImageNode(nodeId = NodeId(2L))).thenReturn(videoImageNode)
        val nodeUpdate = NodeUpdate(update)

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            val updatedNodes = awaitItem()
            assertThat(updatedNodes.size).isEqualTo(2)
            assertThat(updatedNodes).contains(videoImageNode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that nodes not in collection are filtered out`() = runTest {
        val nodeIds = listOf(NodeId(1L))
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(initialNodes[0])

        val imageNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            imageNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        val nodeUpdate = NodeUpdate(update)

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            assertThat(awaitItem()).isEqualTo(initialNodes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that removed nodes are filtered out`() = runTest {
        val nodeIds = listOf(NodeId(1L), NodeId(2L))
        val initialNodes = listOf(
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            },
            mock<ImageNode> {
                on { id }.thenReturn(NodeId(2L))
                on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
            }
        )

        whenever(photosRepository.fetchImageNode(NodeId(1L), false)).thenReturn(initialNodes[0])
        whenever(photosRepository.fetchImageNode(NodeId(2L), false)).thenReturn(initialNodes[1])

        val fileNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { type }.thenReturn(StaticImageFileTypeInfo("", ""))
        }

        val update = mapOf<Node, List<NodeChanges>>(
            fileNode to listOf(NodeChanges.Parent),
        )

        val nodeUpdateFlow = MutableSharedFlow<NodeUpdate>()
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(nodeUpdateFlow)

        whenever(photosRepository.fetchImageNode(nodeId = NodeId(2L))).thenReturn(null)
        val nodeUpdate = NodeUpdate(update)

        underTest(nodeIds).test {
            assertThat(awaitItem()).isEqualTo(initialNodes)
            nodeUpdateFlow.emit(nodeUpdate)
            val updatedNodes = awaitItem()
            assertThat(updatedNodes.size).isEqualTo(1)
            assertThat(updatedNodes).contains(initialNodes[0])
            cancelAndIgnoreRemainingEvents()
        }
    }
}