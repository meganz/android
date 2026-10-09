package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStore
import mega.privacy.android.feature.sync.domain.usecase.progress.PlaceSyncTransferUseCase.Placement
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlaceSyncTransferUseCaseTest {

    private lateinit var underTest: PlaceSyncTransferUseCase

    @BeforeAll
    fun setUp() {
        underTest = PlaceSyncTransferUseCase()
    }

    @ParameterizedTest(name = "{0} with {1} tracked rows gives {2}")
    @MethodSource("newTransfers")
    fun `test that invoke tracks a new transfer only while the tracked rows have room`(
        state: TransferState,
        activeCount: Int,
        expected: Placement,
    ) {
        val placement = underTest(entry(state), SyncMediaType.Video, null, activeCount = activeCount, trackedLimit = 1)

        assertThat(placement).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} over a parked {1} with {2} tracked rows gives {3}")
    @MethodSource("parkedTransfers")
    fun `test that invoke promotes reparks or keeps an overflow transfer`(
        state: TransferState,
        parkedState: TransferState,
        activeCount: Int,
        expected: Placement,
    ) {
        val placement = underTest(entry(state), SyncMediaType.Video, parked(parkedState), activeCount, trackedLimit = 1)

        assertThat(placement).isEqualTo(expected)
    }

    private fun newTransfers(): Stream<Arguments> = Stream.of(
        Arguments.of(TransferState.STATE_QUEUED, 0, Placement.Track),
        Arguments.of(TransferState.STATE_QUEUED, 1, Placement.Park(parked(TransferState.STATE_QUEUED))),
        Arguments.of(TransferState.STATE_PAUSED, 1, Placement.Park(parked(TransferState.STATE_PAUSED))),
        Arguments.of(TransferState.STATE_ACTIVE, 1, Placement.Track),
        Arguments.of(TransferState.STATE_RETRYING, 1, Placement.Track),
        Arguments.of(TransferState.STATE_COMPLETING, SyncTransferStore.SLOT_HOLDER_HEADROOM, Placement.Track),
        Arguments.of(
            TransferState.STATE_ACTIVE,
            SyncTransferStore.SLOT_HOLDER_HEADROOM + 1,
            Placement.Park(parked(TransferState.STATE_ACTIVE)),
        ),
    )

    private fun parkedTransfers(): Stream<Arguments> = Stream.of(
        Arguments.of(TransferState.STATE_QUEUED, TransferState.STATE_QUEUED, 0, Placement.Track),
        Arguments.of(TransferState.STATE_ACTIVE, TransferState.STATE_QUEUED, 1, Placement.Track),
        Arguments.of(
            TransferState.STATE_PAUSED,
            TransferState.STATE_QUEUED,
            1,
            Placement.Park(parked(TransferState.STATE_PAUSED)),
        ),
        Arguments.of(
            TransferState.STATE_ACTIVE,
            TransferState.STATE_QUEUED,
            SyncTransferStore.SLOT_HOLDER_HEADROOM + 1,
            Placement.Park(parked(TransferState.STATE_ACTIVE)),
        ),
        Arguments.of(TransferState.STATE_QUEUED, TransferState.STATE_QUEUED, 1, Placement.Keep),
    )

    private fun entry(state: TransferState) = SyncTransferEntry(
        tag = 1,
        direction = SyncTransferDirection.Download,
        state = state,
        fileName = "f1.mp4",
        totalBytes = 100L,
        transferredBytes = 0L,
        speed = 0L,
        meanSpeed = 0L,
        priority = 1L,
        notificationNumber = 1L,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private fun parked(state: TransferState) = SyncTransferOverflowEntry(
        direction = SyncTransferDirection.Download,
        state = state,
        mediaType = SyncMediaType.Video,
    )
}
