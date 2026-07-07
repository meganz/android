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
class SignalMediaCountChangesUseCaseTest {

    private lateinit var underTest: SignalMediaCountChangesUseCase

    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = SignalMediaCountChangesUseCase(monitorNodeUpdatesUseCase)
    }

    @AfterEach
    fun tearDown() {
        reset(monitorNodeUpdatesUseCase)
    }

    @Test
    fun `test that a section-affecting change on a media node emits`() = runTest {
        stubUpdates(NodeUpdate(mapOf(imageNode() to listOf(NodeChanges.New))))

        assertThat(underTest().toList()).hasSize(1)
    }

    @Test
    fun `test that all section-affecting change types emit for a media node`() = runTest {
        val node = imageNode()
        val sectionAffecting = listOf(
            NodeChanges.New, // Add
            NodeChanges.Remove, // Remove
            NodeChanges.Parent, // Move to trash
            NodeChanges.Timestamp, // Move section
            NodeChanges.Sensitive // Removed or Added if hidden item setting is disabled
        )
        stubUpdates(*sectionAffecting.map { NodeUpdate(mapOf(node to listOf(it))) }.toTypedArray())

        assertThat(underTest().toList()).hasSize(sectionAffecting.size)
    }

    @Test
    fun `test that a non-section change on a media node does not emit`() = runTest {
        val node = imageNode()
        stubUpdates(
            NodeUpdate(mapOf(node to listOf(NodeChanges.Name))),
            NodeUpdate(mapOf(node to listOf(NodeChanges.Favourite))),
        )

        assertThat(underTest().toList()).isEmpty()
    }

    @Test
    fun `test that a section-affecting change on an SVG node does not emit`() = runTest {
        val svgNode = mock<FileNode> {
            on { type } doReturn SvgFileTypeInfo(mimeType = "image/svg+xml", extension = "svg")
        }
        stubUpdates(NodeUpdate(mapOf(svgNode to listOf(NodeChanges.New))))

        assertThat(underTest().toList()).isEmpty()
    }

    @Test
    fun `test that a section-affecting change on a non-media node does not emit`() = runTest {
        val pdfNode = mock<FileNode> { on { type } doReturn PdfFileTypeInfo }
        stubUpdates(NodeUpdate(mapOf(pdfNode to listOf(NodeChanges.New))))

        assertThat(underTest().toList()).isEmpty()
    }

    private fun imageNode() = mock<FileNode> {
        on { type } doReturn StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
    }

    private fun stubUpdates(vararg updates: NodeUpdate) {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf(*updates))
    }
}
