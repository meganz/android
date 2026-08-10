package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.math.BigInteger

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetActiveTransfersAsFinishedUseCaseTest {

    private lateinit var underTest: SetActiveTransfersAsFinishedUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetActiveTransfersAsFinishedUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that transfers are set as finished`(
        cancelled: Boolean,
    ) = runTest {
        val transfers = (0..5L).map {
            createActiveTransfer(
                uniqueId = it,
                state = TransferState.STATE_COMPLETED
            )
        }
        val expected = transfers.map {
            it.copy(
                isFinished = true,
                state = if (cancelled) TransferState.STATE_CANCELLED else it.state
            )
        }

        underTest(transfers, cancelled)

        verify(transferRepository).putActiveTransfers(expected)
    }

    private fun createActiveTransfer(
        uniqueId: Long = 5L,
        isFinished: Boolean = false,
        state: TransferState = TransferState.STATE_ACTIVE,
    ) = Transfer(
        uniqueId = uniqueId,
        transferType = TransferType.DOWNLOAD,
        startTime = 0L,
        transferredBytes = 100L,
        totalBytes = 1000L,
        localPath = "/path/to/file",
        parentPath = "",
        nodeHandle = 0L,
        parentHandle = 0L,
        fileName = "test_file.txt",
        stage = TransferStage.STAGE_NONE,
        tag = 1,
        folderTransferTag = null,
        speed = 0L,
        isSyncTransfer = false,
        isBackupTransfer = false,
        isForeignOverQuota = false,
        isStreamingTransfer = false,
        isFinished = isFinished,
        isFolderTransfer = false,
        appData = emptyList(),
        state = state,
        priority = BigInteger.ZERO,
        notificationNumber = 0L,
    )
}
