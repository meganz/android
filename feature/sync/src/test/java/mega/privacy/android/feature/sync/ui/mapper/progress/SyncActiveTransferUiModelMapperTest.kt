package mega.privacy.android.feature.sync.ui.mapper.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveTransferUiModel
import mega.privacy.android.feature.sync.ui.progress.active
import mega.privacy.android.feature.sync.ui.progress.blockers
import mega.privacy.android.feature.sync.ui.progress.entry
import mega.privacy.android.feature.sync.ui.progress.rowState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncActiveTransferUiModelMapperTest {

    private lateinit var underTest: SyncActiveTransferUiModelMapper

    @BeforeEach
    fun setUp() {
        underTest = SyncActiveTransferUiModelMapper()
    }

    @Test
    fun `test that invoke copies the identity of the row`() {
        val result = map(
            entry = entry(
                tag = 7,
                fileName = "a.pdf",
                direction = SyncTransferDirection.Download,
            ),
            rowState = rowState(status = SyncTransferStatus.Queued),
        )

        assertThat(result.tag).isEqualTo(7)
        assertThat(result.fileName).isEqualTo("a.pdf")
        assertThat(result.direction).isEqualTo(SyncTransferDirection.Download)
        assertThat(result.status).isEqualTo(SyncTransferStatus.Queued)
    }

    @Test
    fun `test that invoke takes the status from the row state, not the list item`() {
        val result = map(
            itemStatus = SyncTransferStatus.Queued,
            rowState = rowState(status = SyncTransferStatus.Transferring),
        )

        assertThat(result.status).isEqualTo(SyncTransferStatus.Transferring)
    }

    @Test
    fun `test that invoke copies the band it is given`() {
        assertThat(map(band = 2).band).isEqualTo(2)
    }

    @Test
    fun `test that invoke does not read the item status for the band`() {
        val result = map(itemStatus = SyncTransferStatus.Paused, band = 1)

        assertThat(result.band).isEqualTo(1)
    }

    @Test
    fun `test that invoke copies the bytes of the row`() {
        val result = map(entry = entry(tag = 1, transferredBytes = 400L, totalBytes = 1_000L))

        assertThat(result.transferredBytes).isEqualTo(400L)
        assertThat(result.totalBytes).isEqualTo(1_000L)
    }

    @Test
    fun `test that invoke copies the progress from the row state`() {
        assertThat(map(rowState = rowState(progressPermille = 456)).progressPermille)
            .isEqualTo(456)
    }

    @Test
    fun `test that invoke caps the progress at full when the row state reports more`() {
        assertThat(map(rowState = rowState(progressPermille = 1_200)).progressPermille)
            .isEqualTo(1000)
    }

    @Test
    fun `test that invoke copies the own pause from the row state`() {
        val result = map(
            rowState = rowState(status = SyncTransferStatus.Paused, isOwnPause = true),
        )

        assertThat(result.isOwnPause).isTrue()
    }

    @Test
    fun `test that invoke does not read the app wide pause for the own pause`() {
        val result = map(
            rowState = rowState(status = SyncTransferStatus.PausedInQueue, isOwnPause = false),
            blockers = blockers(areTransfersPaused = true),
        )

        assertThat(result.isOwnPause).isFalse()
    }

    @Test
    fun `test that invoke copies the display speed from the row state`() {
        assertThat(map(rowState = rowState(speed = 100L)).speed).isEqualTo(100L)
    }

    @Test
    fun `test that invoke copies the eta from the row state`() {
        assertThat(map(rowState = rowState(etaSeconds = 6L)).etaSeconds).isEqualTo(6L)
    }

    @Test
    fun `test that invoke copies a missing eta from the row state`() {
        assertThat(map(rowState = rowState(etaSeconds = null)).etaSeconds).isNull()
    }

    @Test
    fun `test that invoke marks storage over quota for an upload when the account storage is full`() {
        val result = map(
            entry = entry(tag = 1, direction = SyncTransferDirection.Upload),
            blockers = blockers(isStorageOverQuota = true),
        )

        assertThat(result.isStorageOverQuota).isTrue()
        assertThat(result.hasIssues).isTrue()
    }

    @Test
    fun `test that invoke does not mark storage over quota for a download when the account storage is full`() {
        val result = map(
            entry = entry(tag = 1, direction = SyncTransferDirection.Download),
            blockers = blockers(isStorageOverQuota = true),
        )

        assertThat(result.isStorageOverQuota).isFalse()
    }

    @Test
    fun `test that invoke marks transfer over quota when the row state reports out of transfer quota`() {
        val result = map(rowState = rowState(status = SyncTransferStatus.OutOfTransferQuota))

        assertThat(result.isTransferOverQuota).isTrue()
        assertThat(result.hasIssues).isTrue()
    }

    @Test
    fun `test that invoke marks transfer over quota for a download when the account transfer quota is exceeded`() {
        val download = map(
            entry = entry(tag = 1, direction = SyncTransferDirection.Download),
            rowState = rowState(status = SyncTransferStatus.Queued),
            blockers = blockers(isTransferOverQuota = true),
        )
        val upload = map(
            entry = entry(tag = 2, direction = SyncTransferDirection.Upload),
            rowState = rowState(status = SyncTransferStatus.Queued),
            blockers = blockers(isTransferOverQuota = true),
        )

        assertThat(download.isTransferOverQuota).isTrue()
        assertThat(upload.isTransferOverQuota).isFalse()
    }

    @Test
    fun `test that invoke does not mark storage over quota for a foreign over quota row`() {
        val result = map(
            entry = entry(tag = 1, isForeignOverQuota = true),
            rowState = rowState(status = SyncTransferStatus.OutOfStorageSpace),
        )

        assertThat(result.isStorageOverQuota).isFalse()
        assertThat(result.status).isEqualTo(SyncTransferStatus.OutOfStorageSpace)
    }

    @Test
    fun `test that invoke reports issues when offline`() {
        val result = map(blockers = blockers(isOffline = true))

        assertThat(result.hasIssues).isTrue()
        assertThat(result.isStorageOverQuota).isFalse()
        assertThat(result.isTransferOverQuota).isFalse()
    }

    @Test
    fun `test that invoke makes a completing row not draggable`() {
        assertThat(map(rowState = rowState(status = SyncTransferStatus.Completing)).isDraggable)
            .isFalse()
        assertThat(map(rowState = rowState(status = SyncTransferStatus.Queued)).isDraggable)
            .isTrue()
    }

    @Test
    fun `test that invoke exposes the last error code of a retrying row`() {
        val result = map(
            entry = entry(tag = 1, lastError = SyncTransferError(code = -3, value = 0)),
            rowState = rowState(status = SyncTransferStatus.Retrying),
        )

        assertThat(result.lastErrorCode).isEqualTo(-3)
    }

    @Test
    fun `test that invoke exposes the last error code of an over quota row`() {
        val result = map(
            entry = entry(tag = 1, lastError = SyncTransferError(code = -17, value = 1)),
            rowState = rowState(status = SyncTransferStatus.OutOfTransferQuota),
        )

        assertThat(result.lastErrorCode).isEqualTo(-17)
    }

    @Test
    fun `test that invoke hides a stale last error of a transferring row`() {
        val result = map(
            entry = entry(tag = 1, lastError = SyncTransferError(code = -3, value = 0)),
            rowState = rowState(status = SyncTransferStatus.Transferring),
        )

        assertThat(result.lastErrorCode).isNull()
    }

    @Test
    fun `test that invoke marks storage over quota when the row state reports out of storage space`() {
        val result = map(
            entry = entry(
                tag = 1,
                direction = SyncTransferDirection.Upload,
                state = TransferState.STATE_RETRYING,
                lastError = SyncTransferError(code = -17, value = 0),
            ),
            rowState = rowState(status = SyncTransferStatus.OutOfStorageSpace),
        )

        assertThat(result.isStorageOverQuota).isTrue()
        assertThat(result.hasIssues).isTrue()
        assertThat(result.lastErrorCode).isEqualTo(-17)
    }

    @Test
    fun `test that invoke marks transfer over quota for a download when a retrying row is over transfer quota`() {
        val result = map(
            entry = entry(tag = 1, direction = SyncTransferDirection.Download),
            rowState = rowState(status = SyncTransferStatus.Queued),
            isRetryingTransferOverQuota = true,
        )

        assertThat(result.isTransferOverQuota).isTrue()
        assertThat(result.hasIssues).isTrue()
    }

    @Test
    fun `test that invoke does not mark transfer over quota for an upload when a retrying row is over transfer quota`() {
        val result = map(
            entry = entry(tag = 1, direction = SyncTransferDirection.Upload),
            rowState = rowState(status = SyncTransferStatus.Queued),
            isRetryingTransferOverQuota = true,
        )

        assertThat(result.isTransferOverQuota).isFalse()
    }

    private fun map(
        entry: SyncTransferEntry = entry(tag = 1),
        itemStatus: SyncTransferStatus = SyncTransferStatus.Transferring,
        rowState: SyncTransferRowState = rowState(),
        blockers: SyncProgressBlockers = blockers(),
        band: Int = 1,
        isRetryingTransferOverQuota: Boolean = false,
    ): SyncActiveTransferUiModel = underTest(
        item = active(entry = entry, status = itemStatus),
        rowState = rowState,
        blockers = blockers,
        band = band,
        isRetryingTransferOverQuota = isRetryingTransferOverQuota,
    )
}
