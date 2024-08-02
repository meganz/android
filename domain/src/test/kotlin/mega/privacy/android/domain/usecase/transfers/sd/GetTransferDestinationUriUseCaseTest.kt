package mega.privacy.android.domain.usecase.transfers.sd

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTransferDestinationUriUseCaseTest {

    private lateinit var underTest: GetTransferDestinationUriUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetTransferDestinationUriUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class, mode = EnumSource.Mode.EXCLUDE, names = ["DOWNLOAD"])
    fun `test that null is returned when is not a download transfer`(
        type: TransferType,
    ) = runTest {
        val transfer = mock<Transfer> {
            on { it.transferType } doReturn type
        }
        assertThat(underTest(transfer)).isNull()
    }

    @Test
    fun `test that null is returned when is a sync transfer`() = runTest {
        val transfer = mock<Transfer> {
            on { it.transferType } doReturn TransferType.DOWNLOAD
            on { it.isSyncTransfer } doReturn true
        }
        assertThat(underTest(transfer)).isNull()
    }

    @Test
    fun `test that transfer's targetUri is returned when it's a root transfer`() = runTest {
        val expected = "destinationUri"
        val transfer = mock<Transfer> {
            on { it.transferType } doReturn TransferType.DOWNLOAD
            on { it.isRootTransfer } doReturn true
            on { it.appData } doReturn listOf(TransferAppData.SdCardDownload("path", expected))
        }

        val actual = underTest(transfer)

        assertThat(actual?.destinationUri).isEqualTo(expected)
        assertThat(actual?.subFolders).isEmpty()
    }

    @Test
    fun `test that database sd transfer's targetUri  and sub folders are returned when it's not a root transfer`() =
        runTest {
            val expected = "destinationUri"
            val expectedSubFolder = "subFolder"
            val cachePath = "cache/path"
            val folderTag = 12
            val transfer = mock<Transfer> {
                on { it.transferType } doReturn TransferType.DOWNLOAD
                on { it.isRootTransfer } doReturn false
                on { it.folderTransferTag } doReturn folderTag
                on { it.parentPath } doReturn "$cachePath/$expectedSubFolder"
            }
            val parentTransfer = mock<SdTransfer> {
                on { it.appData } doReturn listOf(
                    TransferAppData.SdCardDownload(
                        expected,
                        expected
                    )
                )
                on { it.path } doReturn cachePath
            }
            whenever(transferRepository.getSdTransferByTag(folderTag)) doReturn parentTransfer
            val actual = underTest(transfer)

            assertThat(actual?.destinationUri).isEqualTo(expected)
            assertThat(actual?.subFolders).isEqualTo(listOf(expectedSubFolder))
        }
}