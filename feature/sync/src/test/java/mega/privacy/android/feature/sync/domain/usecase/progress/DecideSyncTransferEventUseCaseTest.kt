package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.usecase.progress.DecideSyncTransferEventUseCase.EventStep
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DecideSyncTransferEventUseCaseTest {

    private lateinit var underTest: DecideSyncTransferEventUseCase

    private val stored = SyncTransferRow(entry(TransferState.STATE_QUEUED, n = 5L), SyncMediaType.Video, null)

    private val parked = SyncTransferOverflowEntry(
        direction = SyncTransferDirection.Download,
        state = TransferState.STATE_QUEUED,
        mediaType = SyncMediaType.Audio,
    )

    @BeforeAll
    fun setUp() {
        underTest = DecideSyncTransferEventUseCase(MergeSyncTransferRowUseCase())
    }

    @ParameterizedTest
    @ValueSource(longs = [4L, 5L])
    fun `test that invoke ignores an event that is not newer than the tracked row`(eventNumber: Long) {
        val step = underTest(finish(TransferState.STATE_COMPLETED, n = eventNumber), stored, null, false)

        assertThat(step).isEqualTo(EventStep.Stale)
    }

    @ParameterizedTest
    @EnumSource(SyncTransferEventType::class)
    fun `test that invoke completes a tracked row on any event carrying the completed state`(
        type: SyncTransferEventType,
    ) {
        val step = underTest(event(type, TransferState.STATE_COMPLETED, n = 6L), stored, null, false)

        assertThat(step).isEqualTo(EventStep.Complete(SyncMediaType.Video))
    }

    @ParameterizedTest
    @EnumSource(names = ["STATE_FAILED", "STATE_CANCELLED", "STATE_ACTIVE"])
    fun `test that invoke rolls back a tracked row on a finish that did not complete`(state: TransferState) {
        val step = underTest(finish(state, n = 6L), stored, null, false)

        assertThat(step).isEqualTo(EventStep.RollBack(SyncTransferDirection.Upload, SyncMediaType.Video))
    }

    @Test
    fun `test that invoke merges a newer non terminal event into the tracked row`() {
        val newer = entry(TransferState.STATE_ACTIVE, n = 6L, transferred = 30L)

        val step = underTest(SyncTransferEvent(SyncTransferEventType.Update, newer, null), stored, null, false)

        assertThat(step).isEqualTo(EventStep.Merge(stored.copy(entry = newer)))
    }

    @Test
    fun `test that invoke completes a parked tag whatever its notification number`() {
        val step = underTest(finish(TransferState.STATE_COMPLETED, n = 1L), null, parked, false)

        assertThat(step).isEqualTo(EventStep.Complete(SyncMediaType.Audio))
    }

    @ParameterizedTest
    @EnumSource(names = ["STATE_FAILED", "STATE_CANCELLED"])
    fun `test that invoke rolls back a parked tag that did not complete`(state: TransferState) {
        val step = underTest(finish(state, n = 1L), null, parked, false)

        assertThat(step).isEqualTo(EventStep.RollBack(SyncTransferDirection.Download, SyncMediaType.Audio))
    }

    @Test
    fun `test that invoke places a non terminal event for a parked tag whatever its notification number`() {
        val event = event(SyncTransferEventType.Update, TransferState.STATE_ACTIVE, n = 1L)

        val step = underTest(event, null, parked, false)

        assertThat(step).isEqualTo(EventStep.Place)
    }

    @ParameterizedTest(name = "{0} {1} started at {2}, removed by a reseed {3}, gives {4}")
    @MethodSource("unknownTagEvents")
    fun `test that invoke decides an event for an unknown tag as the fold table says`(
        type: SyncTransferEventType,
        state: TransferState,
        startNotificationNumber: Long?,
        isRemovedByReseed: Boolean,
        expected: EventStep,
    ) {
        val event = event(type, state, n = 6L, startNotificationNumber = startNotificationNumber)

        val step = underTest(event, null, null, isRemovedByReseed)

        assertThat(step).isEqualTo(expected)
    }

    private fun unknownTagEvents(): Stream<Arguments> = Stream.of(
        Arguments.of(FINISH, TransferState.STATE_COMPLETED, 3L, false, EventStep.AdmitAndComplete(3L)),
        Arguments.of(FINISH, TransferState.STATE_COMPLETED, null, true, EventStep.AdmitAndComplete(6L)),
        Arguments.of(FINISH, TransferState.STATE_COMPLETED, 3L, true, EventStep.AdmitAndComplete(3L)),
        Arguments.of(FINISH, TransferState.STATE_COMPLETED, null, false, EventStep.Ignore),
        Arguments.of(FINISH, TransferState.STATE_FAILED, null, true, EventStep.Ignore),
        Arguments.of(FINISH, TransferState.STATE_CANCELLED, 3L, false, EventStep.Ignore),
        Arguments.of(FINISH, TransferState.STATE_ACTIVE, 3L, false, EventStep.Ignore),
        Arguments.of(SyncTransferEventType.Start, TransferState.STATE_QUEUED, 3L, false, EventStep.Admit(3L)),
        Arguments.of(SyncTransferEventType.Update, TransferState.STATE_ACTIVE, null, false, EventStep.Admit(6L)),
        Arguments.of(SyncTransferEventType.Update, TransferState.STATE_PAUSED, null, true, EventStep.Admit(6L)),
    )

    private fun finish(state: TransferState, n: Long) = event(FINISH, state, n)

    private fun event(
        type: SyncTransferEventType,
        state: TransferState,
        n: Long,
        startNotificationNumber: Long? = null,
    ) = SyncTransferEvent(type, entry(state, n), startNotificationNumber)

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

    private companion object {
        val FINISH = SyncTransferEventType.Finish
    }
}
