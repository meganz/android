package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MergeSyncTransferRowUseCaseTest {

    private lateinit var underTest: MergeSyncTransferRowUseCase

    @BeforeAll
    fun setUp() {
        underTest = MergeSyncTransferRowUseCase()
    }

    @ParameterizedTest(name = "{0} of {1} then {2} of {3} keeps {4}")
    @MethodSource("monotonicBytes")
    fun `test that invoke keeps the transferred bytes monotonic while the total size is unchanged`(
        storedTransferred: Long,
        storedTotal: Long,
        newTransferred: Long,
        newTotal: Long,
        expectedTransferred: Long,
    ) {
        val stored = row(entry(TransferState.STATE_ACTIVE, transferred = storedTransferred, total = storedTotal))
        val newer = entry(TransferState.STATE_ACTIVE, n = 2L, transferred = newTransferred, total = newTotal)
            .copy(speed = 7L)

        val merged = underTest(stored, newer)

        assertThat(merged.entry).isEqualTo(newer.copy(transferredBytes = expectedTransferred))
    }

    @Test
    fun `test that invoke keeps the media type of the stored row`() {
        val stored = row(entry(TransferState.STATE_QUEUED))

        val merged = underTest(stored, entry(TransferState.STATE_ACTIVE, n = 2L).copy(fileName = "x.mp4"))

        assertThat(merged.mediaType).isEqualTo(SyncMediaType.Image)
    }

    @ParameterizedTest(name = "{0} then {1} gives {2}")
    @MethodSource("optimisticPauseMerges")
    fun `test that invoke resolves the optimistic pause as the merge table says`(
        optimisticBefore: Boolean?,
        newState: TransferState,
        expected: Boolean?,
    ) {
        val stored = row(entry(TransferState.STATE_ACTIVE), optimisticPause = optimisticBefore)

        val merged = underTest(stored, entry(newState, n = 2L))

        assertThat(merged.optimisticPause).isEqualTo(expected)
    }

    private fun monotonicBytes(): Stream<Arguments> = Stream.of(
        Arguments.of(60L, 100L, 40L, 100L, 60L),
        Arguments.of(60L, 100L, 80L, 100L, 80L),
        Arguments.of(60L, 100L, 10L, 200L, 10L),
    )

    private fun optimisticPauseMerges(): Stream<Arguments> = Stream.of(
        Arguments.of(true, TransferState.STATE_PAUSED, null),
        Arguments.of(true, TransferState.STATE_COMPLETING, null),
        Arguments.of(true, TransferState.STATE_ACTIVE, true),
        Arguments.of(true, TransferState.STATE_QUEUED, true),
        Arguments.of(false, TransferState.STATE_QUEUED, null),
        Arguments.of(false, TransferState.STATE_ACTIVE, null),
        Arguments.of(false, TransferState.STATE_PAUSED, false),
        Arguments.of(null, TransferState.STATE_PAUSED, null),
        Arguments.of(null, TransferState.STATE_ACTIVE, null),
    )

    private fun row(entry: SyncTransferEntry, optimisticPause: Boolean? = null) =
        SyncTransferRow(entry, SyncMediaType.Image, optimisticPause)

    private fun entry(
        state: TransferState,
        n: Long = 1L,
        transferred: Long = 0L,
        total: Long = 100L,
    ) = SyncTransferEntry(
        tag = 1,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = "f1.jpg",
        totalBytes = total,
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
