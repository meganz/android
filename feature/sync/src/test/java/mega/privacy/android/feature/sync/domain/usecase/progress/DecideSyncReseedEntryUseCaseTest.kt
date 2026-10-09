package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.usecase.progress.DecideSyncReseedEntryUseCase.ReseedStep
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DecideSyncReseedEntryUseCaseTest {

    private lateinit var underTest: DecideSyncReseedEntryUseCase

    private val stored = SyncTransferRow(entry(TransferState.STATE_QUEUED, n = 7L), SyncMediaType.Image, null)

    private val parked = SyncTransferOverflowEntry(SyncTransferDirection.Upload, TransferState.STATE_QUEUED)

    @BeforeAll
    fun setUp() {
        underTest = DecideSyncReseedEntryUseCase(MergeSyncTransferRowUseCase())
    }

    @ParameterizedTest
    @EnumSource(names = ["STATE_COMPLETED", "STATE_FAILED", "STATE_CANCELLED"])
    fun `test that invoke skips a terminal entry before anything else`(state: TransferState) {
        val step = underTest(entry(state, n = 9L), isCompletedTag = true, stored = stored, parked = null)

        assertThat(step).isEqualTo(ReseedStep.Terminal)
    }

    @Test
    fun `test that invoke leaves a tag that already completed unchanged`() {
        val entry = entry(TransferState.STATE_QUEUED, n = 9L)

        val step = underTest(entry, isCompletedTag = true, stored = null, parked = null)

        assertThat(step).isEqualTo(ReseedStep.Unchanged)
    }

    @Test
    fun `test that invoke merges a newer entry into the tracked row`() {
        val newer = entry(TransferState.STATE_ACTIVE, n = 9L, transferred = 50L)

        val step = underTest(newer, isCompletedTag = false, stored = stored, parked = null)

        assertThat(step).isEqualTo(ReseedStep.Merge(stored.copy(entry = newer)))
    }

    @ParameterizedTest
    @ValueSource(longs = [6L, 7L])
    fun `test that invoke keeps a tracked row that is not older than the entry`(entryNumber: Long) {
        val entry = entry(TransferState.STATE_ACTIVE, n = entryNumber)

        val step = underTest(entry, isCompletedTag = false, stored = stored, parked = null)

        assertThat(step).isEqualTo(ReseedStep.Unchanged)
    }

    @Test
    fun `test that invoke places an entry for a parked tag`() {
        val entry = entry(TransferState.STATE_ACTIVE, n = 1L)

        val step = underTest(entry, isCompletedTag = false, stored = null, parked = parked)

        assertThat(step).isEqualTo(ReseedStep.Place)
    }

    @Test
    fun `test that invoke admits an entry for an unknown tag`() {
        val entry = entry(TransferState.STATE_QUEUED, n = 9L)

        val step = underTest(entry, isCompletedTag = false, stored = null, parked = null)

        assertThat(step).isEqualTo(ReseedStep.Admit)
    }

    private fun entry(state: TransferState, n: Long, transferred: Long = 0L) = SyncTransferEntry(
        tag = 1,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = "f1.jpg",
        totalBytes = 100L,
        transferredBytes = transferred,
        speed = 0L,
        meanSpeed = 0L,
        priority = 1L,
        notificationNumber = n,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )
}
