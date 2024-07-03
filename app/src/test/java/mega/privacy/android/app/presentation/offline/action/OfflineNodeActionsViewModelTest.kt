package mega.privacy.android.app.presentation.offline.action


import android.content.Intent
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.app.presentation.offline.action.model.OfflineNodeActionUiEntity
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFilesUseCase
import org.junit.jupiter.api.AfterEach
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
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineNodeActionsViewModelTest {
    private val getOfflineFileUseCase: GetOfflineFileUseCase = mock()
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase = mock()
    private val exportNodesUseCase: ExportNodesUseCase = mock()
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase = mock()
    private val snackBarHandler: SnackBarHandler = mock()
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper = mock()

    private lateinit var underTest: OfflineNodeActionsViewModel

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private suspend fun stubCommon() {
        whenever(getOfflineFilesUseCase(any())).thenReturn(mapOf())
        whenever(exportNodesUseCase(any())).thenReturn(mapOf())
    }

    private fun initViewModel() {
        underTest = OfflineNodeActionsViewModel(
            getOfflineFileUseCase = getOfflineFileUseCase,
            getOfflineFilesUseCase = getOfflineFilesUseCase,
            exportNodesUseCase = exportNodesUseCase,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            snackBarHandler = snackBarHandler,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper
        )
    }

    @Test
    fun `test that handleShareOfflineNodes emits shareFilesEvent when all nodes are files`() =
        runTest {
            val file = mock<File>()
            val offlineFileInformation: OfflineFileInformation = mock {
                on { id } doReturn 123
                on { isFolder } doReturn false
            }
            whenever(getOfflineFilesUseCase(listOf(offlineFileInformation))).thenReturn(
                mapOf(123 to file)
            )

            underTest.handleShareOfflineNodes(listOf(offlineFileInformation), true)
            verify(getOfflineFilesUseCase).invoke(any())

            underTest.uiState.test {
                val res = awaitItem()
                assertThat(res.shareFilesEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.shareFilesEvent as StateEventWithContentTriggered).content)
                    .containsExactly(file)
            }
        }

    @Test
    fun `test that handleShareOfflineNodes calls exportNodesUseCase when a node is folder and online`() =
        runTest {
            val offlineFileInformation: OfflineFileInformation = mock()
            whenever(offlineFileInformation.isFolder).thenReturn(true)
            whenever(offlineFileInformation.handle).thenReturn("123")
            underTest.handleShareOfflineNodes(listOf(offlineFileInformation), true)
            verify(exportNodesUseCase).invoke(any())
        }

    @Test
    fun `test that sharesNodeLinksEvent is sent with correct title and formatted links when node is folder`() =
        runTest {
            val offlineFileInformation = mock<OfflineFileInformation> {
                on { name } doReturn "name1"
                on { isFolder } doReturn true
                on { handle } doReturn "123"
            }
            val offlineFileInformation2: OfflineFileInformation = mock<OfflineFileInformation> {
                on { name } doReturn "name2"
                on { isFolder } doReturn true
                on { handle } doReturn "456"
            }
            whenever(exportNodesUseCase(listOf(123, 456))).thenReturn(
                mapOf(
                    123L to "link1",
                    456L to "link2"
                )
            )
            underTest.handleShareOfflineNodes(
                listOf(
                    offlineFileInformation,
                    offlineFileInformation2
                ), true
            )

            underTest.uiState.test {
                val res = awaitItem()
                assertThat(res.sharesNodeLinksEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.sharesNodeLinksEvent as StateEventWithContentTriggered).content)
                    .isEqualTo("name1" to "link1\n\nlink2")
            }
        }

    @Test
    fun `test that handleShareOfflineNodes post snackbar error message when offline and node is a folder`() =
        runTest {
            val offlineFileInformation: OfflineFileInformation = mock()
            whenever(offlineFileInformation.isFolder).thenReturn(true)
            whenever(offlineFileInformation.handle).thenReturn("123")
            underTest.handleShareOfflineNodes(listOf(offlineFileInformation), false)
            verify(snackBarHandler).postSnackbarMessage(R.string.error_server_connection_problem)
        }

    @ParameterizedTest(name = "File type is {0}")
    @MethodSource("provideOfflineNodeType")
    fun `test that invoke is called when offline node is provided with different file types`(
        type: FileTypeInfo,
        expected: OfflineNodeActionUiEntity,
    ) = runTest {
        val nodeInfo = mock<OfflineFileInformation>().stub {
            on { handle } doReturn "123"
            on { path } doReturn "path"
            on { name } doReturn "name"
            on { fileTypeInfo } doReturn type
        }

        val file = File("path")
        val content = NodeContentUri.LocalContentUri(file)
        whenever(getOfflineFileUseCase(nodeInfo)).thenReturn(file)

        underTest.handleOpenOfflineFile(nodeInfo)

        when (type) {
            is UrlFileTypeInfo -> {
                verify(getPathFromNodeContentUseCase).invoke(content)
            }

            else -> {
                verifyNoMoreInteractions(getPathFromNodeContentUseCase)
            }
        }

        underTest.uiState.test {
            val res = awaitItem()
            assertThat(res.openFileEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((res.openFileEvent as StateEventWithContentTriggered).content)
                .isInstanceOf(expected::class.java)
        }
    }

    @Test
    fun `test that text file is considered as other file type when size is too large`() =
        runTest {
            val nodeInfo = mock<OfflineFileInformation>().stub {
                on { handle } doReturn "123"
                on { path } doReturn "path"
                on { name } doReturn "name"
                on { fileTypeInfo } doReturn TextFileTypeInfo("text/plain", "txt")
            }

            val file = mock<File> {
                on { length() } doReturn TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE + 100L
            }
            whenever(getOfflineFileUseCase(nodeInfo)).thenReturn(file)

            underTest.handleOpenOfflineFile(nodeInfo)

            underTest.uiState.test {
                val res = awaitItem()
                assertThat(res.openFileEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.openFileEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(OfflineNodeActionUiEntity.Other::class.java)
            }
        }

    @Test
    fun `test that URL file type is handled when handleOpenOfflineFile is invoked`() =
        runTest {
            val nodeInfo = mock<OfflineFileInformation>().stub {
                on { handle } doReturn "123"
                on { path } doReturn "path"
                on { name } doReturn "name"
                on { fileTypeInfo } doReturn UrlFileTypeInfo
            }
            val file = mock<File>()
            whenever(getOfflineFileUseCase(nodeInfo)).thenReturn(file)

            underTest.handleOpenOfflineFile(nodeInfo)

            verify(getPathFromNodeContentUseCase).invoke(NodeContentUri.LocalContentUri(file))
            underTest.uiState.test {
                val res = awaitItem()
                assertThat(res.openFileEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.openFileEvent as StateEventWithContentTriggered).content)
                    .isInstanceOf(OfflineNodeActionUiEntity.Uri::class.java)
            }
        }

    private fun provideOfflineNodeType() = Stream.of(
        Arguments.of(
            PdfFileTypeInfo,
            mock<OfflineNodeActionUiEntity.Pdf>()
        ),
        Arguments.of(
            VideoFileTypeInfo(
                extension = "mp4",
                mimeType = "video",
                duration = Duration.INFINITE
            ),
            mock<OfflineNodeActionUiEntity.AudioOrVideo>()
        ),
        Arguments.of(
            AudioFileTypeInfo(
                extension = "mp3",
                mimeType = "audio",
                duration = Duration.INFINITE
            ),
            mock<OfflineNodeActionUiEntity.AudioOrVideo>()
        ),
        Arguments.of(
            StaticImageFileTypeInfo(
                extension = "jpeg",
                mimeType = "image",
            ),
            mock<OfflineNodeActionUiEntity.Image>()
        ),
        Arguments.of(
            TextFileTypeInfo(
                mimeType = "text/plain",
                extension = "txt"
            ),
            mock<OfflineNodeActionUiEntity.Text>()
        ),
        Arguments.of(
            UrlFileTypeInfo,
            mock<OfflineNodeActionUiEntity.Uri>()
        ),
        Arguments.of(
            ZipFileTypeInfo(
                mimeType = "application/zip",
                extension = "zip"
            ),
            mock<OfflineNodeActionUiEntity.Zip>()
        ),
        Arguments.of(
            UnknownFileTypeInfo(
                mimeType = "abc",
                extension = "abc"
            ),
            mock<OfflineNodeActionUiEntity.Other>()
        ),
    )

    @Test
    fun `test that applyNodeContentUri calls correct mapper when invoked`() = runTest {
        val file = mock<File>()
        val nodeContentUri = NodeContentUri.LocalContentUri(file)
        val intent = mock<Intent>()

        underTest.applyNodeContentUri(intent, file, "audio", true)

        verify(nodeContentUriIntentMapper).invoke(intent, nodeContentUri, "audio", true)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflineFilesUseCase,
            exportNodesUseCase,
            snackBarHandler,
            nodeContentUriIntentMapper,
            getOfflineFileUseCase,
            getPathFromNodeContentUseCase
        )
    }
}