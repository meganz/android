package mega.privacy.android.domain.usecase.transfers.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroupImpl
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DestinationAndAppDataForDownloadResult
import mega.privacy.android.domain.usecase.transfers.downloads.GetFileDestinationAndAppDataForDownloadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InsertPendingDownloadsForNodesUseCaseTest {
    private lateinit var underTest: InsertPendingDownloadsForNodesUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getPendingTransferNodeIdentifierUseCase =
        mock<GetPendingTransferNodeIdentifierUseCase>()
    private val doesUriPathHaveSufficientSpaceForNodesUseCase =
        mock<DoesUriPathHaveSufficientSpaceForNodesUseCase>()
    private val getFileDestinationAndAppDataForDownloadUseCase =
        mock<GetFileDestinationAndAppDataForDownloadUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val timeSystemRepository = mock<TimeSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = InsertPendingDownloadsForNodesUseCase(
            transferRepository,
            getPendingTransferNodeIdentifierUseCase,
            doesUriPathHaveSufficientSpaceForNodesUseCase,
            getFileDestinationAndAppDataForDownloadUseCase,
            fileSystemRepository,
            timeSystemRepository
        )
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(
            transferRepository,
            getPendingTransferNodeIdentifierUseCase,
            doesUriPathHaveSufficientSpaceForNodesUseCase,
            getFileDestinationAndAppDataForDownloadUseCase,
            fileSystemRepository,
            timeSystemRepository,
        )
        val nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(647L))
        whenever(getPendingTransferNodeIdentifierUseCase(anyOrNull())) doReturn nodeIdentifier
        whenever(getFileDestinationAndAppDataForDownloadUseCase(UriPath(PATH_STRING))) doReturn
                DestinationAndAppDataForDownloadResult(UriPath(PATH_STRING), null)
        whenever(transferRepository.insertActiveTransferGroup(any())) doReturn GROUP_ID
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that an InsertPendingTransferRequest is sent to transferRepository's insertPendingTransfers with the correct parameters for each node`(
        isHighPriority: Boolean,
    ) = runTest {
        val nodes = (0..5).map { index ->
            mock<DefaultTypedFileNode> {
                on { it.name } doReturn "fileName$index"
            }
        }
        val appData = TransferAppData.OfflineDownload
        val destinationAppData = mock<TransferAppData.SdCardDownload>()
        val expectedAppData =
            listOf(appData, destinationAppData, TransferAppData.TransferGroup(GROUP_ID))
        whenever(getFileDestinationAndAppDataForDownloadUseCase(UriPath(PATH_STRING))) doReturn
                DestinationAndAppDataForDownloadResult(UriPath(PATH_STRING), destinationAppData)
        val uriPath = UriPath(PATH_STRING)
        val expected = nodes.mapIndexed { index, node ->
            val nodeIdentifier =
                PendingTransferNodeIdentifier.CloudDriveNode(NodeId(index.toLong()))
            whenever(getPendingTransferNodeIdentifierUseCase(node)) doReturn nodeIdentifier
            whenever(
                doesUriPathHaveSufficientSpaceForNodesUseCase(uriPath, nodes)
            ) doReturn true
            InsertPendingTransferRequest(
                transferType = TransferType.DOWNLOAD,
                nodeIdentifier = nodeIdentifier,
                uriPath = uriPath,
                appData = expectedAppData,
                isHighPriority = isHighPriority,
                fileName = "fileName$index",
            )
        }

        underTest(nodes, uriPath, isHighPriority, appData)

        verify(transferRepository).insertPendingTransfers(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that active transfer group is created with correct parameters`(
        multipleNodes: Boolean,
    ) = runTest {
        val singleFileName = "file.txt"
        val time = 349853489L
        val nodes = if (multipleNodes) (0..5).map { index ->
            mock<DefaultTypedFileNode>()
        } else {
            listOf(mock<DefaultTypedFileNode> {
                on { name } doReturn singleFileName
            })
        }
        val uriPath = UriPath(PATH_STRING)
        whenever(
            doesUriPathHaveSufficientSpaceForNodesUseCase(uriPath, nodes)
        ) doReturn true
        whenever(timeSystemRepository.getCurrentTimeInMillis()) doReturn time
        underTest(
            nodes = nodes,
            destination = uriPath,
            isHighPriority = false,
            appData = null,
        )
        verify(transferRepository).insertActiveTransferGroup(
            ActiveTransferGroupImpl(
                transferType = TransferType.DOWNLOAD,
                destination = uriPath.value,
                singleFileName = if (multipleNodes) null else singleFileName,
                startTime = time
            )
        )
    }

    @Test
    fun `test that a NotEnoughStorageException is thrown when doesPathHaveSufficientSpaceForNodesUseCase returns false`() =
        runTest {
            val node = mock<DefaultTypedFileNode>()
            val destination = UriPath(PATH_STRING)
            whenever(
                doesUriPathHaveSufficientSpaceForNodesUseCase(destination, listOf(node))
            ) doReturn false

            assertThrows<NotEnoughStorageException> {
                underTest(listOf(node), destination, false, null)
            }
        }

    @Test
    fun `test that an exception thrown by getDestinationForSdkAndAppDataForDownloadUseCase is propagated`() =
        runTest {
            val node = mock<DefaultTypedFileNode>()
            val destination = PATH_STRING
            val expected = RuntimeException()
            whenever(getFileDestinationAndAppDataForDownloadUseCase(UriPath(destination))) doThrow expected

            val actual = runCatching {
                underTest(listOf(node), UriPath(destination), false, null)
            }.exceptionOrNull()

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that folder is created`() = runTest {
        val uriPath = UriPath(PATH_STRING)
        val nodes = listOf(mock<DefaultTypedFileNode>())
        whenever(
            doesUriPathHaveSufficientSpaceForNodesUseCase(uriPath, nodes)
        ) doReturn true

        underTest(nodes, uriPath, false, null)

        verify(fileSystemRepository).createDirectory(PATH_STRING)
    }

    companion object {
        private const val FOLDER_NAME = "uriPath"
        private const val PATH_STRING = "$FOLDER_NAME/"
        private const val GROUP_ID = 874L
    }
}