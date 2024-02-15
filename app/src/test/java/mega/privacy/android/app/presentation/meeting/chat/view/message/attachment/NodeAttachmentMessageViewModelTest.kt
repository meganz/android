package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.usecase.node.chat.AddChatFileTypeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAttachmentMessageViewModelTest {
    lateinit var underTest: NodeAttachmentMessageViewModel

    private val getPreviewUseCase = mock<GetPreviewUseCase>()
    private val addChatFileTypeUseCase = mock<AddChatFileTypeUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()

    @BeforeAll
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    internal fun initTests() {
        resetMocks()
        underTest = NodeAttachmentMessageViewModel(
            getPreviewUseCase,
            addChatFileTypeUseCase,
            fileSizeStringMapper,
            durationInSecondsTextMapper,
        )
    }

    fun resetMocks() {
        reset(
            getPreviewUseCase,
            addChatFileTypeUseCase,
            fileSizeStringMapper,
            durationInSecondsTextMapper,
        )
    }

    @Test
    fun `test that preview is added when message is added`() = runTest {
        val expectedType = UnknownFileTypeInfo(
            mimeType = "application/jpg",
            extension = "jpg"
        )
        val fileNode = mock<FileNode> {
            on { hasPreview } doReturn true
            on { name } doReturn "name"
            on { size } doReturn 123L
            on { type } doReturn expectedType
        }
        whenever(fileSizeStringMapper(any())).thenReturn("size")
        val chatFile = mock<ChatDefaultFile>()
        val expected = "file://image.jpg"
        val previewFile = mock<File> {
            on { toString() } doReturn expected
        }
        whenever(addChatFileTypeUseCase(fileNode, CHAT_ID, MSG_ID)).thenReturn(chatFile)
        whenever(getPreviewUseCase(chatFile)).thenReturn(previewFile)
        val msg = buildNodeAttachmentMessage(fileNode)
        underTest.getOrPutUiStateFlow(msg, CHAT_ID).test {
            val actual = awaitItem().previewUri
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that ui state initial state has correct name and size`() = runTest {
        val expectedName = "attachment.jpg"
        val expectedSize = "a lot of bytes"
        val expectedType = UnknownFileTypeInfo(
            mimeType = "application/jpg",
            extension = "jpg"
        )
        val fileNode = mock<FileNode> {
            on { name } doReturn expectedName
            on { size } doReturn 123L
            on { hasPreview } doReturn false
            on { type } doReturn expectedType
        }
        whenever(fileSizeStringMapper(any())).thenReturn(expectedSize)

        val msg = buildNodeAttachmentMessage(fileNode)
        underTest.getOrPutUiStateFlow(msg, CHAT_ID).test {
            val actual = awaitItem()
            assertThat(actual.fileName).isEqualTo(expectedName)
            assertThat(actual.fileSize).isEqualTo(expectedSize)
        }
    }

    private fun buildNodeAttachmentMessage(fileNode: FileNode) =
        NodeAttachmentMessage(
            chatId = CHAT_ID,
            msgId = MSG_ID,
            time = 12L,
            isMine = true,
            userHandle = 23L,
            shouldShowAvatar = true,
            shouldShowTime = true,
            fileNode = fileNode,
            reactions = emptyList(),
        )
}

private const val CHAT_ID = 44L
private const val MSG_ID = 55L