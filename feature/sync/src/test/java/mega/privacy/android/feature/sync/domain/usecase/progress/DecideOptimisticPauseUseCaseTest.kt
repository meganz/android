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
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DecideOptimisticPauseUseCaseTest {

    private lateinit var underTest: DecideOptimisticPauseUseCase

    @BeforeAll
    fun setUp() {
        underTest = DecideOptimisticPauseUseCase()
    }

    @ParameterizedTest(name = "{0}, pause {1} gives {2}")
    @MethodSource("pauseRequests")
    fun `test that invoke sets the optimistic state as the pause table says`(
        row: SyncTransferRow,
        isPaused: Boolean,
        expected: SyncTransferRow?,
    ) {
        assertThat(underTest(row, isPaused)).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that invoke keeps a completing row whatever the request`(isPaused: Boolean) {
        val next = underTest(row(TransferState.STATE_COMPLETING, null), isPaused)

        assertThat(next).isNull()
    }

    @ParameterizedTest(name = "{0} with optimistic {1} reverts to none")
    @MethodSource("revertibleRows")
    fun `test that invoke clears a pending optimistic state on revert`(
        state: TransferState,
        optimisticPause: Boolean,
    ) {
        val next = underTest(row(state, optimisticPause), requestedPause = null)

        assertThat(next).isEqualTo(row(state, null))
    }

    @Test
    fun `test that invoke keeps a row without an optimistic state on revert`() {
        val next = underTest(row(TransferState.STATE_ACTIVE, null), requestedPause = null)

        assertThat(next).isNull()
    }

    private fun pauseRequests(): Stream<Arguments> = Stream.of(
        Arguments.of(row(TransferState.STATE_PAUSED, null), true, null),
        Arguments.of(row(TransferState.STATE_QUEUED, null), false, null),
        Arguments.of(row(TransferState.STATE_ACTIVE, null), true, row(TransferState.STATE_ACTIVE, true)),
        Arguments.of(row(TransferState.STATE_PAUSED, null), false, row(TransferState.STATE_PAUSED, false)),
        Arguments.of(row(TransferState.STATE_ACTIVE, true), false, row(TransferState.STATE_ACTIVE, false)),
        Arguments.of(row(TransferState.STATE_PAUSED, false), true, row(TransferState.STATE_PAUSED, true)),
        Arguments.of(row(TransferState.STATE_ACTIVE, true), true, null),
    )

    private fun revertibleRows(): Stream<Arguments> = Stream.of(
        Arguments.of(TransferState.STATE_ACTIVE, true),
        Arguments.of(TransferState.STATE_ACTIVE, false),
        Arguments.of(TransferState.STATE_COMPLETING, true),
    )

    private fun row(state: TransferState, optimisticPause: Boolean?) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = 1,
            direction = SyncTransferDirection.Upload,
            state = state,
            fileName = "f1.jpg",
            totalBytes = 100L,
            transferredBytes = 0L,
            speed = 0L,
            meanSpeed = 0L,
            priority = 1L,
            notificationNumber = 1L,
            lastError = null,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Image,
        optimisticPause = optimisticPause,
    )
}
