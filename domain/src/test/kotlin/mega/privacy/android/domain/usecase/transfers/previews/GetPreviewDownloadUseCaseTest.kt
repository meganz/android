package mega.privacy.android.domain.usecase.transfers.previews

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPreviewDownloadUseCaseTest {

    private lateinit var underTest: GetPreviewDownloadUseCase

    private val transferRepository = mock<TransferRepository>()

    private val nodeHandle = 123L
    private val nodeID = NodeId(nodeHandle)
    private val fileName = "fileName"
    private val node = mock<TypedFileNode> {
        on { id } doReturn nodeID
        on { name } doReturn fileName
    }
    private val previewAppData = listOf(TransferAppData.PreviewDownload)

    @BeforeAll
    fun setup() {
        underTest = GetPreviewDownloadUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that transfer is returned when found`() = runTest {
        val foundActiveTransferId = 1L
        val foundActiveTransfer = mock<ActiveTransfer> {
            on { appData } doReturn previewAppData
            on { this.fileName } doReturn fileName
            on { uniqueId } doReturn foundActiveTransferId
        }
        val expectedTransfer = mock<Transfer> {
            on { this.nodeHandle } doReturn nodeHandle
        }
        val activeDownloads = buildList {
            add(foundActiveTransfer)
            add(mock<ActiveTransfer> {
                on { appData } doReturn previewAppData
                on { fileName } doReturn "otherName"
                on { uniqueId } doReturn 2
            })
            add(mock<ActiveTransfer> {
                on { appData } doReturn emptyList()
                on { fileName } doReturn "otherName"
                on { uniqueId } doReturn 3
            })
        }
        val previewDownloads = activeDownloads
            .filter { it.isPreviewDownload() && it.fileName == fileName }

        whenever(transferRepository.getCurrentActiveTransfersByType(TransferType.DOWNLOAD)) doReturn activeDownloads
        previewDownloads.forEach { activeDownload ->
            val expected = if (activeDownload.uniqueId == foundActiveTransferId) {
                expectedTransfer
            } else {
                null
            }
            whenever(transferRepository.getTransferByUniqueId(activeDownload.uniqueId)) doReturn expected
        }

        assertThat(underTest.invoke(node)).isEqualTo(expectedTransfer)
    }

    @Test
    fun `test that null is returned if transfer is not found`() = runTest {
        val foundActiveTransferId = 1L
        val foundActiveTransfer = mock<ActiveTransfer> {
            on { appData } doReturn previewAppData
            on { this.fileName } doReturn fileName
            on { uniqueId } doReturn foundActiveTransferId
        }
        val transfer = mock<Transfer> {
            on { this.nodeHandle } doReturn 321
        }
        val activeDownloads = buildList {
            add(foundActiveTransfer)
            add(mock<ActiveTransfer> {
                on { appData } doReturn previewAppData
                on { fileName } doReturn "otherName"
                on { uniqueId } doReturn 2
            })
            add(mock<ActiveTransfer> {
                on { appData } doReturn emptyList()
                on { fileName } doReturn "otherName"
                on { uniqueId } doReturn 3
            })
        }
        val previewDownloads = activeDownloads
            .filter { it.isPreviewDownload() && it.fileName == fileName }

        whenever(transferRepository.getCurrentActiveTransfersByType(TransferType.DOWNLOAD)) doReturn activeDownloads
        previewDownloads.forEach { activeDownload ->
            val expected = if (activeDownload.uniqueId == foundActiveTransferId) {
                transfer
            } else {
                null
            }

            whenever(transferRepository.getTransferByUniqueId(activeDownload.uniqueId)) doReturn expected
        }

        assertThat(underTest.invoke(node)).isNull()
    }

    @Test
    fun `test that null is returned if there are no download transfers`() = runTest {
        whenever(transferRepository.getCurrentActiveTransfersByType(TransferType.DOWNLOAD)) doReturn emptyList()

        assertThat(underTest.invoke(node)).isNull()
    }
}