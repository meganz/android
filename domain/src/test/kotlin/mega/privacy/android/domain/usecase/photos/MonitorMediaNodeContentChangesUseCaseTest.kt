package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorMediaNodeContentChangesUseCaseTest {

    private lateinit var underTest: MonitorMediaNodeContentChangesUseCase

    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorMediaNodeContentChangesUseCase(monitorNodeUpdatesUseCase)
    }

    @AfterEach
    fun tearDown() {
        reset(monitorNodeUpdatesUseCase)
    }

    @Test
    fun `test that a content change on a media node emits its id`() = runTest {
        val node = imageNode(id = 7L)
        stubUpdates(NodeUpdate(mapOf(node to listOf(NodeChanges.Favourite))))

        assertThat(underTest().toList()).containsExactly(setOf(7L))
    }

    @Test
    fun `test that both favourite and sensitive changes emit`() = runTest {
        val favourite = imageNode(id = 1L)
        val sensitive = imageNode(id = 2L)
        stubUpdates(
            NodeUpdate(mapOf(favourite to listOf(NodeChanges.Favourite))),
            NodeUpdate(mapOf(sensitive to listOf(NodeChanges.Sensitive))),
        )

        assertThat(underTest().toList()).containsExactly(setOf(1L), setOf(2L)).inOrder()
    }

    @Test
    fun `test that a structural-only change does not emit`() = runTest {
        val node = imageNode(id = 5L)
        stubUpdates(
            NodeUpdate(mapOf(node to listOf(NodeChanges.New))),
            NodeUpdate(mapOf(node to listOf(NodeChanges.Name))),
        )

        assertThat(underTest().toList()).isEmpty()
    }

    @Test
    fun `test that a content change on an SVG node does not emit`() = runTest {
        val svg = mock<FileNode> {
            on { id } doReturn NodeId(9L)
            on { type } doReturn SvgFileTypeInfo(mimeType = "image/svg+xml", extension = "svg")
        }
        stubUpdates(NodeUpdate(mapOf(svg to listOf(NodeChanges.Favourite))))

        assertThat(underTest().toList()).isEmpty()
    }

    @Test
    fun `test that a content change on a non-media node does not emit`() = runTest {
        val pdf = mock<FileNode> {
            on { id } doReturn NodeId(11L)
            on { type } doReturn PdfFileTypeInfo
        }
        stubUpdates(NodeUpdate(mapOf(pdf to listOf(NodeChanges.Favourite))))

        assertThat(underTest().toList()).isEmpty()
    }

    private fun imageNode(id: Long) = mock<FileNode> {
        on { this.id } doReturn NodeId(id)
        on { type } doReturn StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
    }

    private fun stubUpdates(vararg updates: NodeUpdate) {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf(*updates))
    }
}
