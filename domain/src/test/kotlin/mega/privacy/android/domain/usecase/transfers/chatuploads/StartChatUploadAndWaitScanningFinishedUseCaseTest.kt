package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartChatUploadAndWaitScanningFinishedUseCaseTest {
    private lateinit var underTest: StartChatUploadAndWaitScanningFinishedUseCase

    private val uploadFileUseCase = mock<UploadFileUseCase>()
    private val getOrCreateMyChatsFilesFolderIdUseCase =
        mock<GetOrCreateMyChatsFilesFolderIdUseCase>()

    @BeforeAll
    fun setup() {
        underTest = StartChatUploadAndWaitScanningFinishedUseCase(
            uploadFileUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            uploadFileUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
        )
        whenever(getOrCreateMyChatsFilesFolderIdUseCase()) doReturn myChatsFolderId
    }

    @Test
    fun `test that upload files use case is invoked when the use case is invoked`() = runTest {
        val file = file
        val fileName = "name"
        val pendingMessageIds = listOf(1L, 2L, 3L)
        whenever(
            uploadFileUseCase(
                uriPath = anyValueClass(),
                fileName = any(),
                appData = any(),
                parentFolderId = anyValueClass(),
                isHighPriority = any(),
            )
        ) doReturn emptyFlow()
        val appData = pendingMessageIds.map {
            TransferAppData.ChatUpload(it)
        }

        underTest.invoke(
            uriPath = UriPath(file.absolutePath),
            fileName = fileName,
            pendingMessageIds = pendingMessageIds
        )

        // Then
        verify(uploadFileUseCase).invoke(
            uriPath = UriPath(file.absolutePath),
            fileName = fileName,
            appData = appData,
            parentFolderId = myChatsFolderId,
            isHighPriority = false,
        )
    }

    @Test
    fun `test that the scanning finished event is awaited when the use case is invoked`() {
        runTest {
            assertDoesNotThrow {
                val finishEvent = mock<TransferEvent.TransferStartEvent> {
                    on { this.transfer } doReturn fileTransfer
                }
                val file = file
                val fileName = "name"
                val pendingMessageIds = listOf(1L, 2L, 3L)
                whenever(
                    uploadFileUseCase(
                        uriPath = anyValueClass(),
                        fileName = any(),
                        appData = anyOrNull(),
                        parentFolderId = anyValueClass(),
                        isHighPriority = any(),
                    )
                ) doReturn flow {
                    emit(finishEvent)
                    throw RuntimeException()
                }

                underTest(
                    uriPath = UriPath(file.absolutePath),
                    fileName = fileName,
                    pendingMessageIds = pendingMessageIds
                )
            }
        }
    }

    @Test
    fun `test that extra app data is added to upload files use case`() = runTest {
        val file = file
        val fileName = "name"
        val pendingMessageIds = listOf(1L, 2L, 3L)
        whenever(
            uploadFileUseCase(
                uriPath = anyValueClass(),
                fileName = any(),
                appData = any(),
                parentFolderId = anyValueClass(),
                isHighPriority = any(),
            )
        ) doReturn emptyFlow()
        val originalUriPath = TransferAppData.OriginalUriPath(UriPath("original/path"))
        val appData = pendingMessageIds.map {
            TransferAppData.ChatUpload(it)
        } + originalUriPath

        underTest.invoke(
            uriPath = UriPath(file.absolutePath),
            fileName = fileName,
            pendingMessageIds = pendingMessageIds,
            extraAppData = originalUriPath
        )

        // Then
        verify(uploadFileUseCase).invoke(
            uriPath = UriPath(file.absolutePath),
            fileName = fileName,
            appData = appData,
            parentFolderId = myChatsFolderId,
            isHighPriority = false,
        )
    }

    private val myChatsFolderId = NodeId(45L)
    private val file = File("/root/file.txt")
    private val fileTransfer = mock<Transfer> {
        on { isFolderTransfer } doReturn false
    }
}
