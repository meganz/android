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
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeActionHandlerViewModelTest {
    private lateinit var viewModel: NodeActionHandlerViewModel

    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val getNodePreviewFileUseCase = mock<GetNodePreviewFileUseCase>()
    private val getPathFromNodeContentUseCase = mock<GetPathFromNodeContentUseCase>()
    private val nodeContentUriIntentMapper = mock<NodeContentUriIntentMapper>()

    @BeforeEach
    fun setUp() {
        viewModel = NodeActionHandlerViewModel(
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
        )
    }

    @ParameterizedTest(name = "File type is {0}")
    @MethodSource("provideNodeType")
    fun `test that handleFileNodeClicked returns correct FileNodeContent for different file types`(
        node: TypedFileNode,
        expected: FileNodeContent,
    ) = runTest {
        val content = NodeContentUri.LocalContentUri(File("path"))
        whenever(getNodeContentUriUseCase(node)).thenReturn(content)
        whenever(getNodePreviewFileUseCase(any())).thenReturn(File("path"))

        val actual = viewModel.handleFileNodeClicked(node)

        when (node.type) {
            is ImageFileTypeInfo -> {
                verifyNoMoreInteractions(getNodeContentUriUseCase)
                verifyNoMoreInteractions(getNodePreviewFileUseCase)
            }

            is TextFileTypeInfo -> {
                verifyNoMoreInteractions(getNodeContentUriUseCase)
                verifyNoMoreInteractions(getNodePreviewFileUseCase)
            }

            is UrlFileTypeInfo -> {
                verify(getNodeContentUriUseCase).invoke(node)
                verify(getPathFromNodeContentUseCase).invoke(content)
            }

            is VideoFileTypeInfo,
            is PdfFileTypeInfo,
            is AudioFileTypeInfo,
                -> {
                verify(getNodeContentUriUseCase).invoke(node)
            }

            else -> {
                verify(getNodePreviewFileUseCase).invoke(node)
            }
        }

        assertThat(actual).isInstanceOf(expected::class.java)
    }

    @Test
    fun `test that handleFileNodeClicked returns Pdf content for PDF files`() = runTest {
        val pdfNode = mock<TypedFileNode>().stub {
            on { type } doReturn PdfFileTypeInfo
        }
        val content = NodeContentUri.LocalContentUri(File("document.pdf"))
        whenever(getNodeContentUriUseCase(pdfNode)).thenReturn(content)

        val result = viewModel.handleFileNodeClicked(pdfNode)

        verify(getNodeContentUriUseCase).invoke(pdfNode)
        assertThat(result).isInstanceOf(FileNodeContent.Pdf::class.java)
        assertThat((result as FileNodeContent.Pdf).uri).isEqualTo(content)
    }

    @Test
    fun `test that handleFileNodeClicked returns ImageForNode for image files`() = runTest {
        val imageNode = mock<TypedFileNode>().stub {
            on { type } doReturn StaticImageFileTypeInfo(
                extension = "jpeg",
                mimeType = "image/jpeg"
            )
        }

        val result = viewModel.handleFileNodeClicked(imageNode)

        verifyNoMoreInteractions(getNodeContentUriUseCase)
        verifyNoMoreInteractions(getNodePreviewFileUseCase)
        assertThat(result).isInstanceOf(FileNodeContent.ImageForNode::class.java)
    }

    @Test
    fun `test that handleFileNodeClicked returns TextContent for small text files`() = runTest {
        val textNode = mock<TypedFileNode>().stub {
            on { type } doReturn TextFileTypeInfo(
                mimeType = "text/plain",
                extension = "txt"
            )
            on { size } doReturn TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE.toLong() - 1
        }

        val result = viewModel.handleFileNodeClicked(textNode)

        verifyNoMoreInteractions(getNodeContentUriUseCase)
        verifyNoMoreInteractions(getNodePreviewFileUseCase)
        assertThat(result).isInstanceOf(FileNodeContent.TextContent::class.java)
    }

    @Test
    fun `test that handleFileNodeClicked returns Other for large text files`() = runTest {
        val textNode = mock<TypedFileNode>().stub {
            on { type } doReturn TextFileTypeInfo(
                mimeType = "text/plain",
                extension = "txt"
            )
            on { size } doReturn TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE.toLong() + 1
        }
        val file = File("large.txt")
        whenever(getNodePreviewFileUseCase(textNode)).thenReturn(file)

        val result = viewModel.handleFileNodeClicked(textNode)

        verify(getNodePreviewFileUseCase).invoke(textNode)
        assertThat(result).isInstanceOf(FileNodeContent.Other::class.java)
        assertThat((result as FileNodeContent.Other).localFile).isEqualTo(file)
    }

    @Test
    fun `test that handleFileNodeClicked returns AudioOrVideo for video files`() = runTest {
        val videoNode = mock<TypedFileNode>().stub {
            on { type } doReturn VideoFileTypeInfo(
                extension = "mp4",
                mimeType = "video/mp4",
                duration = Duration.INFINITE
            )
        }
        val content = NodeContentUri.LocalContentUri(File("video.mp4"))
        whenever(getNodeContentUriUseCase(videoNode)).thenReturn(content)

        val result = viewModel.handleFileNodeClicked(videoNode)

        verify(getNodeContentUriUseCase).invoke(videoNode)
        assertThat(result).isInstanceOf(FileNodeContent.AudioOrVideo::class.java)
        assertThat((result as FileNodeContent.AudioOrVideo).uri).isEqualTo(content)
    }

    @Test
    fun `test that handleFileNodeClicked returns AudioOrVideo for audio files`() = runTest {
        val audioNode = mock<TypedFileNode>().stub {
            on { type } doReturn AudioFileTypeInfo(
                extension = "mp3",
                mimeType = "audio/mpeg",
                duration = Duration.INFINITE
            )
        }
        val content = NodeContentUri.LocalContentUri(File("audio.mp3"))
        whenever(getNodeContentUriUseCase(audioNode)).thenReturn(content)

        val result = viewModel.handleFileNodeClicked(audioNode)

        verify(getNodeContentUriUseCase).invoke(audioNode)
        assertThat(result).isInstanceOf(FileNodeContent.AudioOrVideo::class.java)
        assertThat((result as FileNodeContent.AudioOrVideo).uri).isEqualTo(content)
    }

    @Test
    fun `test that handleFileNodeClicked returns UrlContent for URL files`() = runTest {
        val urlNode = mock<TypedFileNode>().stub {
            on { type } doReturn UrlFileTypeInfo
        }
        val content = NodeContentUri.LocalContentUri(File("url.url"))
        val path = "/path/to/url"
        whenever(getNodeContentUriUseCase(urlNode)).thenReturn(content)
        whenever(getPathFromNodeContentUseCase(content)).thenReturn(path)

        val result = viewModel.handleFileNodeClicked(urlNode)

        verify(getNodeContentUriUseCase).invoke(urlNode)
        verify(getPathFromNodeContentUseCase).invoke(content)
        assertThat(result).isInstanceOf(FileNodeContent.UrlContent::class.java)
        assertThat((result as FileNodeContent.UrlContent).uri).isEqualTo(content)
        assertThat(result.path).isEqualTo(path)
    }

    @Test
    fun `test that handleFileNodeClicked returns Other for unknown file types`() = runTest {
        val unknownNode = mock<TypedFileNode>().stub {
            on { type } doReturn UnknownFileTypeInfo(
                mimeType = "application/unknown",
                extension = "unknown"
            )
        }
        val file = File("unknown.unknown")
        whenever(getNodePreviewFileUseCase(unknownNode)).thenReturn(file)

        val result = viewModel.handleFileNodeClicked(unknownNode)

        verify(getNodePreviewFileUseCase).invoke(unknownNode)
        assertThat(result).isInstanceOf(FileNodeContent.Other::class.java)
        assertThat((result as FileNodeContent.Other).localFile).isEqualTo(file)
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

    private fun provideNodeType() = Stream.of(
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn PdfFileTypeInfo
            },
            mock<FileNodeContent.Pdf>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn VideoFileTypeInfo(
                    extension = "mp4",
                    mimeType = "video/mp4",
                    duration = Duration.INFINITE
                )
            },
            mock<FileNodeContent.AudioOrVideo>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn AudioFileTypeInfo(
                    extension = "mp3",
                    mimeType = "audio/mpeg",
                    duration = Duration.INFINITE
                )
            },
            mock<FileNodeContent.AudioOrVideo>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn StaticImageFileTypeInfo(
                    extension = "jpeg",
                    mimeType = "image/jpeg"
                )
            },
            mock<FileNodeContent.ImageForNode>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn TextFileTypeInfo(
                    mimeType = "text/plain",
                    extension = "txt"
                )
                on { size } doReturn TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE.toLong() - 1
            },
            mock<FileNodeContent.TextContent>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn ZipFileTypeInfo(
                    mimeType = "application/zip",
                    extension = "zip"
                )
            },
            mock<FileNodeContent.Other>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn UnknownFileTypeInfo(
                    mimeType = "application/unknown",
                    extension = "unknown"
                )
            },
            mock<FileNodeContent.Other>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn UrlFileTypeInfo
            },
            mock<FileNodeContent.UrlContent>()
        )
    )
}