package mega.privacy.android.core.nodecomponents.action

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeActionHandlerViewModelTest {
    private lateinit var viewModel: NodeActionHandlerViewModel

    private val getFileNodeContentForFileNodeUseCase = mock<GetFileNodeContentForFileNodeUseCase>()
    private val nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>()

    @BeforeEach
    fun setUp() {
        viewModel = NodeActionHandlerViewModel(
            getFileNodeContentForFileNodeUseCase = getFileNodeContentForFileNodeUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
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
}