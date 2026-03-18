package mega.privacy.android.core.nodecomponents.action

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeActionHandlerViewModelTest {
    private lateinit var viewModel: NodeActionHandlerViewModel

    private val getFileNodeContentForFileNodeUseCase = mock<GetFileNodeContentForFileNodeUseCase>()
    private val nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getFileNodeContentForFileNodeUseCase,
            nodeContentUriIntentMapper,
            getFeatureFlagValueUseCase
        )
        viewModel = NodeActionHandlerViewModel(
            getFileNodeContentForFileNodeUseCase = getFileNodeContentForFileNodeUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that applyNodeContentUri calls mapper with correct parameters`() {
        val intent = mock<Intent>()
        val content = NodeContentUri.LocalContentUri(File("test.txt"))
        val mimeType = "text/plain"
        val isSupported = true

        viewModel.applyNodeContentUri(intent, content, mimeType, isSupported)

        verify(nodeContentUriIntentMapper).invoke(intent, content, mimeType, isSupported)
    }

    @Test
    fun `test that applyNodeContentUri calls mapper with default isSupported parameter`() {
        val intent = mock<Intent>()
        val content = NodeContentUri.LocalContentUri(File("test.txt"))
        val mimeType = "text/plain"

        viewModel.applyNodeContentUri(intent, content, mimeType)

        verify(nodeContentUriIntentMapper).invoke(intent, content, mimeType, true)
    }

    @Test
    fun `test that handleFileNodeClicked returns content only`() = runTest {
        val fileNode = createMockFileNode()
        val expectedContent = FileNodeContent.TextContent
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)).thenReturn(expectedContent)

        val result = viewModel.handleFileNodeClicked(fileNode)

        assertThat(result).isEqualTo(expectedContent)
        verify(getFileNodeContentForFileNodeUseCase).invoke(fileNode)
    }

    @Test
    fun `test that state isPDFViewerEnabled emits when flag is loaded`() = runTest {
        reset(getFeatureFlagValueUseCase)
        whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)

        val viewModelUnderTest = NodeActionHandlerViewModel(
            getFileNodeContentForFileNodeUseCase = getFileNodeContentForFileNodeUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )

        val result = viewModelUnderTest.state
            .map { it.isPDFViewerEnabled }
            .first { it != null }

        assertThat(result).isTrue()
        verify(getFeatureFlagValueUseCase).invoke(AppFeatures.PdfViewerComposeUI)
    }

    @Test
    fun `test that initial state has null PDF viewer flag`() = runTest {
        assertThat(viewModel.state.value.isPDFViewerEnabled).isNull()
    }

    private fun createMockFileNode(): TypedFileNode = mock {
        whenever(it.id).thenReturn(NodeId(1L))
        whenever(it.parentId).thenReturn(NodeId(0L))
        whenever(it.name).thenReturn("test.txt")
        whenever(it.type).thenReturn(TextFileTypeInfo("text/plain", "txt"))
    }
}
