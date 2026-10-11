package mega.privacy.android.domain.usecase.transfers.downloads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadNodeUseCaseTest {
    private lateinit var underTest: DownloadNodeUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val transferRepository = mock<TransferRepository>()


    @BeforeAll
    fun setup() {
        underTest = DownloadNodeUseCase(
            fileSystemRepository,
            transferRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            fileSystemRepository,
            transferRepository,
        )
        whenever(
            transferRepository.startDownload(
                any(),
                any(),
                anyOrNull(),
                any()
            )
        ) doReturn emptyFlow()
    }

    @Test
    fun `test that fileSystemRepository createDirectory is invoked`() = runTest {
        val destinationPath = "destinationPath"
        val node = mock<DefaultTypedFileNode>()

        underTest.invoke(node, destinationPath, null, false).test {
            awaitComplete()
        }

        verify(fileSystemRepository).createDirectory(destinationPath)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that transferRepository startDownload is invoked`(
        isHighPriority: Boolean
    ) = runTest {
        val destinationPath = "destinationPath"
        val node = mock<DefaultTypedFileNode>()

        underTest.invoke(node, destinationPath, null, isHighPriority).test {
            awaitComplete()
        }

        verify(transferRepository).startDownload(node, destinationPath, null, isHighPriority)
    }

    @Test
    fun `test that appdata is send to startDownload`() = runTest {
        val destinationPath = "destinationPath"
        val node = mock<DefaultTypedFileNode>()
        val appData = mock<List<TransferAppData>>()

        underTest.invoke(node, destinationPath, appData, false).test {
            awaitComplete()
        }

        verify(transferRepository).startDownload(node, destinationPath, appData, false)
    }

    @Test
    fun `test that chat appdata is added when node is a chat file`() = runTest {
        val destinationPath = "destinationPath"
        val node = mock<ChatDefaultFile> {
            on { chatId } doReturn 4857L
            on { messageId } doReturn 974L
            on { messageIndex } doReturn 347
        }
        val appData = listOf(TransferAppData.OriginalUriPath(UriPath("content uri")))
        val expected = TransferAppData.ChatDownload(
            node.chatId,
            node.messageId,
            node.messageIndex
        )

        underTest.invoke(node, destinationPath, appData, false).test {
            awaitComplete()
        }

        verify(transferRepository).startDownload(eq(node), eq(destinationPath), argThat {
            this.containsAll(appData)
                    && this.any { it == expected }
        }, eq(false))
    }

    @Test
    fun `test that values from transferRepository startDownload are returned`() = runTest {
        val destinationPath = "destinationPath"
        val node = mock<DefaultTypedFileNode>()
        val expectedEvents = listOf(
            mock<TransferEvent.TransferStartEvent>(),
            mock<TransferEvent.TransferUpdateEvent>(),
            mock<TransferEvent.TransferFinishEvent>(),

            )
        whenever(transferRepository.startDownload(node, destinationPath, null, false)) doReturn
                expectedEvents.asFlow()

        underTest.invoke(node, destinationPath, null, false).test {
            expectedEvents.forEach { expectedEvent ->
                val actual = awaitItem()
                assertThat(actual).isEqualTo(expectedEvent)
            }
            awaitComplete()
        }
    }
}