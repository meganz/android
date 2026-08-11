package mega.privacy.android.domain.usecase.transfers.previews

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersFromSdkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancelOverQuotaPreviewDownloadsUseCaseTest {

    private lateinit var underTest: CancelOverQuotaPreviewDownloadsUseCase

    private val getInProgressTransfersFromSdkUseCase =
        mock<GetInProgressTransfersFromSdkUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()

    private val previewDownloadTag = 1
    private val previewDownload = mock<Transfer> {
        on { tag } doReturn previewDownloadTag
        on { appData } doReturn listOf(TransferAppData.PreviewDownload)
    }
    private val otherDownload = mock<Transfer> {
        on { tag } doReturn 2
        on { appData } doReturn emptyList()
    }

    @BeforeAll
    fun setup() {
        underTest = CancelOverQuotaPreviewDownloadsUseCase(
            getInProgressTransfersFromSdkUseCase = getInProgressTransfersFromSdkUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getInProgressTransfersFromSdkUseCase,
            cancelTransferByTagUseCase,
        )
    }

    @Test
    fun `test that only preview downloads are cancelled`() = runTest {
        whenever(getInProgressTransfersFromSdkUseCase()) doReturn
                listOf(previewDownload, otherDownload)

        assertThat(underTest()).containsExactly(previewDownloadTag)

        verify(cancelTransferByTagUseCase)(previewDownloadTag)
    }

    @Test
    fun `test that no transfer is cancelled when there is no preview download in progress`() =
        runTest {
            whenever(getInProgressTransfersFromSdkUseCase()) doReturn listOf(otherDownload)

            assertThat(underTest()).isEmpty()

            verifyNoInteractions(cancelTransferByTagUseCase)
        }
}
