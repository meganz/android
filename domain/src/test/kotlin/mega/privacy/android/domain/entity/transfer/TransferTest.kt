package mega.privacy.android.domain.entity.transfer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.Progress
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferTest {
    lateinit var underTest: Transfer

    @ParameterizedTest
    @EnumSource(value = TransferState::class)
    fun `test that paused returns true only when transfer state is paused`(transferState: TransferState) {
        underTest = createEmptyTransfer().copy(state = transferState)
        val expected = transferState == TransferState.STATE_PAUSED

        assertThat(underTest.isPaused).isEqualTo(expected)
    }

    @Test
    fun `test that isAlreadyDownloaded is true when the conditions are met`() {
        underTest = createEmptyTransfer().copy(
            state = TransferState.STATE_COMPLETED,
            isFinished = true,
            transferredBytes = 0,
        )

        assertThat(underTest.isAlreadyDownloaded).isTrue()
    }

    @ParameterizedTest
    @MethodSource("notIsAlreadyDownloaded")
    fun `test that isAlreadyDownloaded is false when the conditions are not met`() {
        underTest = createAlreadyDownloadedTransfer()

        assertThat(underTest.isAlreadyDownloaded).isTrue()
    }

    @Test
    fun `test that isRootTransfer returns true if folderTransferTag is null`() {
        underTest = createEmptyTransfer().copy(folderTransferTag = null)

        assertThat(underTest.isRootTransfer).isTrue()
    }

    @Test
    fun `test that isRootTransfer returns false if folderTransferTag is not null`() {
        underTest = createEmptyTransfer().copy(folderTransferTag = 1)

        assertThat(underTest.isRootTransfer).isFalse()
    }

    @Test
    fun `test that progress returns the correct progress for total bytes and transferred bytes`() {
        val totalBytes = 1000L
        val transferredBytes = 500L
        underTest = createEmptyTransfer().copy(
            totalBytes = totalBytes,
            transferredBytes = transferredBytes
        )

        assertThat(underTest.progress).isEqualTo(Progress(transferredBytes, totalBytes))
    }

    private fun notIsAlreadyDownloaded() = listOf(
        createAlreadyDownloadedTransfer().copy(state = TransferState.STATE_FAILED),
        createAlreadyDownloadedTransfer().copy(state = TransferState.STATE_CANCELLED),
        createAlreadyDownloadedTransfer().copy(isFinished = false),
        createAlreadyDownloadedTransfer().copy(transferredBytes = 1L),
    )

    private fun createAlreadyDownloadedTransfer() = createEmptyTransfer().copy(
        state = TransferState.STATE_COMPLETED,
        isFinished = true,
        transferredBytes = 0,
    )

    private fun createEmptyTransfer() = Transfer(
        transferType = TransferType.NONE,
        transferredBytes = 0L,
        totalBytes = 0L,
        localPath = "",
        parentPath = "",
        nodeHandle = 0L,
        parentHandle = 0L,
        fileName = "",
        stage = TransferStage.STAGE_NONE,
        tag = 0,
        folderTransferTag = null,
        speed = 0L,
        isSyncTransfer = false,
        isBackupTransfer = false,
        isForeignOverQuota = false,
        isStreamingTransfer = false,
        isFinished = false,
        isFolderTransfer = false,
        appData = emptyList(),
        state = TransferState.STATE_NONE,
        priority = BigInteger.ZERO,
        notificationNumber = 0L,
    )
}