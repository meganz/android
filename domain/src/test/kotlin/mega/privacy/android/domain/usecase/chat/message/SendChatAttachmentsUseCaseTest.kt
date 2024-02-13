package mega.privacy.android.domain.usecase.chat.message

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.usecase.file.GetFileFromUriUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendChatAttachmentsUseCaseTest {
    private lateinit var underTest: SendChatAttachmentsUseCase

    private val startChatUploadsWithWorkerUseCase = mock<StartChatUploadsWithWorkerUseCase>()
    private val getFileFromUriUseCase = mock<GetFileFromUriUseCase>()

    @BeforeAll
    fun setup() {
        underTest = SendChatAttachmentsUseCase(
            startChatUploadsWithWorkerUseCase,
            getFileFromUriUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            startChatUploadsWithWorkerUseCase,
            getFileFromUriUseCase,
        )
        whenever(startChatUploadsWithWorkerUseCase(any(), any())).thenReturn(
            flowOf(MultiTransferEvent.ScanningFoldersFinished)
        )
    }

    @Test
    fun `test that correct file is uploaded`() = runTest {
        val chatId = 123L
        val uris = listOf("file")
        val file = mock<File>()
        whenever(getFileFromUriUseCase(any(), any())).thenReturn(file)
        underTest(chatId, uris).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(startChatUploadsWithWorkerUseCase)(eq(listOf(file)), any())
    }

    @Test
    fun `test that each file is uploaded with chat upload`() = runTest {
        val chatId = 123L
        val uris = List(3) { "file$it" }
        whenever(getFileFromUriUseCase(any(), any())).thenReturn(mock())
        underTest(chatId, uris).test {
            cancelAndIgnoreRemainingEvents()
        }
        verify(startChatUploadsWithWorkerUseCase, times(uris.size))(any(), any())
    }
}