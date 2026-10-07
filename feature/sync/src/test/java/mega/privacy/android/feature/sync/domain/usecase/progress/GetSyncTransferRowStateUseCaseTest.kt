package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncTransferRowStateUseCaseTest {

    private lateinit var underTest: GetSyncTransferRowStateUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetSyncTransferRowStateUseCase()
    }

    @Test
    fun `test that invoke returns OutOfTransferQuota when the error code is the sdk over quota code`() {
        val sdkError = SyncTransferError(MegaError.API_EOVERQUOTA, 1)
        val row = row(entry(TransferState.STATE_RETRYING, error = sdkError))

        assertThat(SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE).isEqualTo(MegaError.API_EOVERQUOTA)
        assertThat(state(row).status).isEqualTo(SyncTransferStatus.OutOfTransferQuota)
    }

    @Test
    fun `test that invoke returns Completing when a completing row is paused`() {
        val row = row(entry(TransferState.STATE_COMPLETING), optimistic = true)

        assertThat(state(row, context(appPaused = true)).status)
            .isEqualTo(SyncTransferStatus.Completing)
    }

    @Test
    fun `test that invoke returns Paused when a paused transfer has bytes`() {
        val row = row(entry(TransferState.STATE_PAUSED, transferred = 10))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Paused)
    }

    @Test
    fun `test that invoke returns PausedInQueue when a paused transfer has no bytes`() {
        val row = row(entry(TransferState.STATE_PAUSED, transferred = 0))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.PausedInQueue)
    }

    @Test
    fun `test that invoke returns PausedInQueue when all transfers are paused and nothing was transferred`() {
        val row = row(entry(TransferState.STATE_QUEUED))

        assertThat(state(row, context(appPaused = true)).status)
            .isEqualTo(SyncTransferStatus.PausedInQueue)
    }

    @Test
    fun `test that invoke returns Paused when all transfers are paused and an active transfer has bytes`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 10))

        assertThat(state(row, context(appPaused = true)).status)
            .isEqualTo(SyncTransferStatus.Paused)
    }

    @ParameterizedTest
    @EnumSource(
        value = TransferState::class,
        names = ["STATE_QUEUED", "STATE_ACTIVE", "STATE_RETRYING", "STATE_COMPLETING"],
    )
    fun `test that invoke returns PausedInQueue only for pausable states when all transfers are paused`(
        transferState: TransferState,
    ) {
        val expected = if (transferState == TransferState.STATE_COMPLETING) {
            SyncTransferStatus.Completing
        } else {
            SyncTransferStatus.PausedInQueue
        }
        val row = row(entry(transferState, transferred = 0))

        assertThat(state(row, context(appPaused = true)).status).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns Paused when an optimistic pause is pending`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 10), optimistic = true)

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Paused)
    }

    @Test
    fun `test that invoke returns Queued when an optimistic resume is pending`() {
        val row = row(entry(TransferState.STATE_PAUSED), optimistic = false)

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Queued)
    }

    @Test
    fun `test that invoke returns Queued when an optimistic resume is pending under the app wide pause`() {
        val row = row(entry(TransferState.STATE_PAUSED), optimistic = false)

        assertThat(state(row, context(appPaused = true)).status)
            .isEqualTo(SyncTransferStatus.Queued)
    }

    @Test
    fun `test that invoke returns OutOfTransferQuota when a retrying transfer has an over quota error with a value`() {
        val row = row(entry(TransferState.STATE_RETRYING, error = overQuota(1)))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.OutOfTransferQuota)
    }

    @Test
    fun `test that invoke returns OutOfStorageSpace when a retrying transfer has an over quota value of zero`() {
        val row = row(entry(TransferState.STATE_RETRYING, error = overQuota(0)))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.OutOfStorageSpace)
    }

    @Test
    fun `test that invoke returns OutOfTransferQuota when a queued transfer has the error and the account is over transfer quota`() {
        val row = row(entry(TransferState.STATE_QUEUED, error = overQuota(1)))

        assertThat(state(row, context(transfer = true)).status)
            .isEqualTo(SyncTransferStatus.OutOfTransferQuota)
    }

    @Test
    fun `test that invoke returns OutOfStorageSpace when a queued upload has a foreign over quota`() {
        val row = row(entry(TransferState.STATE_QUEUED, error = overQuota(0), foreign = true))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.OutOfStorageSpace)
    }

    @Test
    fun `test that invoke returns Queued when the stale over quota error no longer matches a blocker`() {
        val transferRow = row(entry(TransferState.STATE_QUEUED, error = overQuota(1)))
        val storageRow = row(entry(TransferState.STATE_QUEUED, error = overQuota(0)))

        assertThat(state(transferRow, context(transfer = false)).status)
            .isEqualTo(SyncTransferStatus.Queued)
        assertThat(state(storageRow, context(storage = false)).status)
            .isEqualTo(SyncTransferStatus.Queued)
    }

    @Test
    fun `test that invoke returns Retrying when a retrying transfer has another error`() {
        val row = row(entry(TransferState.STATE_RETRYING, error = SyncTransferError(-3, 0)))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Retrying)
    }

    @Test
    fun `test that invoke returns Retrying when a retrying transfer has no error`() {
        val row = row(entry(TransferState.STATE_RETRYING, error = null))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Retrying)
    }

    @ParameterizedTest
    @EnumSource(value = TransferState::class, names = ["STATE_QUEUED", "STATE_NONE"])
    fun `test that invoke returns Queued when the state is queued or unset`(
        transferState: TransferState,
    ) {
        assertThat(state(row(entry(transferState))).status).isEqualTo(SyncTransferStatus.Queued)
    }

    @Test
    fun `test that invoke returns Starting when an active transfer has no bytes`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 0))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Starting)
    }

    @Test
    fun `test that invoke returns Transferring when an active transfer has bytes`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 1))

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.Transferring)
    }

    @Test
    fun `test that invoke returns isOwnPause true when the sdk paused the transfer`() {
        assertThat(state(row(entry(TransferState.STATE_PAUSED))).isOwnPause).isTrue()
    }

    @ParameterizedTest
    @EnumSource(
        value = TransferState::class,
        names = ["STATE_QUEUED", "STATE_ACTIVE", "STATE_RETRYING", "STATE_COMPLETING"],
    )
    fun `test that invoke returns isOwnPause false when the sdk has not paused the transfer`(
        transferState: TransferState,
    ) {
        assertThat(state(row(entry(transferState))).isOwnPause).isFalse()
    }

    @Test
    fun `test that invoke returns isOwnPause false when only the app wide pause applies`() {
        val queued = state(row(entry(TransferState.STATE_QUEUED)), context(appPaused = true))
        val active = state(row(entry(TransferState.STATE_ACTIVE)), context(appPaused = true))

        assertThat(queued.status).isEqualTo(SyncTransferStatus.PausedInQueue)
        assertThat(queued.isOwnPause).isFalse()
        assertThat(active.isOwnPause).isFalse()
    }

    @Test
    fun `test that invoke returns isOwnPause from the optimistic state when one is pending`() {
        val pausing = row(entry(TransferState.STATE_ACTIVE), optimistic = true)
        val resuming = row(entry(TransferState.STATE_PAUSED), optimistic = false)

        assertThat(state(pausing).isOwnPause).isTrue()
        assertThat(state(resuming).isOwnPause).isFalse()
    }

    @Test
    fun `test that invoke returns the lower speed when a direction speed is known`() {
        val fast = row(entry(TransferState.STATE_ACTIVE, transferred = 10, speed = 500))
        val slow = row(entry(TransferState.STATE_ACTIVE, transferred = 10, speed = 200))

        assertThat(state(fast, directionSpeed = 300).speed).isEqualTo(300)
        assertThat(state(slow, directionSpeed = 300).speed).isEqualTo(200)
    }

    @Test
    fun `test that invoke returns the row speed when no direction speed is known`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 10, speed = 500))

        assertThat(state(row, directionSpeed = null).speed).isEqualTo(500)
    }

    @Test
    fun `test that invoke returns speed zero when the transfer is completing`() {
        val row = row(entry(TransferState.STATE_COMPLETING, speed = 500))

        assertThat(state(row).speed).isEqualTo(0)
    }

    @Test
    fun `test that invoke returns the mean speed when the transfer is completed`() {
        val row = row(entry(TransferState.STATE_COMPLETED, meanSpeed = 700))

        assertThat(state(row).speed).isEqualTo(700)
    }

    @Test
    fun `test that invoke returns the total bytes as speed when a completed transfer has no mean speed`() {
        val row = row(entry(TransferState.STATE_COMPLETED, total = 4_096, meanSpeed = 0))

        assertThat(state(row).speed).isEqualTo(4_096)
    }

    @Test
    fun `test that invoke returns the time left from the capped speed when a direction speed is known`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 10, total = 1_010, speed = 500))

        assertThat(state(row, directionSpeed = 100).etaSeconds).isEqualTo(10)
        assertThat(state(row, directionSpeed = 0).etaSeconds).isNull()
    }

    @Test
    fun `test that invoke returns a floored progress when the bytes do not divide evenly`() {
        val third = row(entry(TransferState.STATE_ACTIVE, transferred = 1, total = 3))
        val almost = row(entry(TransferState.STATE_ACTIVE, transferred = 999, total = 1_000))

        assertThat(state(third).progressPermille).isEqualTo(333)
        assertThat(state(almost).progressPermille).isEqualTo(999)
    }

    @ParameterizedTest
    @EnumSource(value = TransferState::class, names = ["STATE_COMPLETING", "STATE_COMPLETED"])
    fun `test that invoke returns progress 1000 when the transfer is completing or completed`(
        transferState: TransferState,
    ) {
        val row = row(entry(transferState, transferred = 5, total = 100))

        assertThat(state(row).progressPermille).isEqualTo(1000)
    }

    @Test
    fun `test that invoke returns progress zero when the total is zero`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 0, total = 0))

        assertThat(state(row).progressPermille).isEqualTo(0)
    }

    @Test
    fun `test that invoke returns a floored eta when the row is transferring`() {
        val row =
            row(entry(TransferState.STATE_ACTIVE, transferred = 1, total = 1_001, speed = 300))

        assertThat(state(row).etaSeconds).isEqualTo(3L)
    }

    @Test
    fun `test that invoke returns no eta when the speed is zero`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 1, total = 1_000, speed = 0))

        assertThat(state(row).etaSeconds).isNull()
    }

    @ParameterizedTest
    @MethodSource("rowsWithAStatusOtherThanTransferring")
    fun `test that invoke returns no eta when the status is not Transferring`(
        expectedStatus: SyncTransferStatus,
        row: SyncTransferRow,
    ) {
        val actual = state(row)

        assertThat(actual.status).isEqualTo(expectedStatus)
        assertThat(actual.etaSeconds).isNull()
    }

    @Test
    fun `test that invoke returns no eta when nothing remains`() {
        val row =
            row(entry(TransferState.STATE_ACTIVE, transferred = 100, total = 100, speed = 300))

        assertThat(state(row).etaSeconds).isNull()
    }

    @ParameterizedTest
    @EnumSource(TransferState::class)
    fun `test that invoke returns a status changed by the app wide pause only when the state is pausable`(
        transferState: TransferState,
    ) {
        val isPausable = transferState in setOf(
            TransferState.STATE_QUEUED,
            TransferState.STATE_ACTIVE,
            TransferState.STATE_RETRYING,
        )
        val row = row(entry(transferState, transferred = 0))

        val running = state(row, context(appPaused = false)).status
        val paused = state(row, context(appPaused = true)).status

        if (isPausable) {
            assertThat(paused).isEqualTo(SyncTransferStatus.PausedInQueue)
            assertThat(running).isNotEqualTo(SyncTransferStatus.PausedInQueue)
        } else {
            assertThat(paused).isEqualTo(running)
        }
    }

    @Test
    fun `test that invoke returns a progress floored rather than rounded when close to the next step`() {
        val twoThirds = row(entry(TransferState.STATE_ACTIVE, transferred = 2, total = 3))
        val almost = row(entry(TransferState.STATE_ACTIVE, transferred = 9_995, total = 10_000))

        assertThat(state(twoThirds).progressPermille).isEqualTo(666)
        assertThat(state(almost).progressPermille).isEqualTo(999)
    }

    @Test
    fun `test that invoke returns a floored eta when the remaining time is not whole`() {
        val row =
            row(entry(TransferState.STATE_ACTIVE, transferred = 1, total = 1_001, speed = 600))

        assertThat(state(row).etaSeconds).isEqualTo(1L)
    }

    @Test
    fun `test that invoke returns OutOfStorageSpace when a queued transfer has the error and the account is over storage quota`() {
        val row = row(entry(TransferState.STATE_QUEUED, error = overQuota(0), foreign = false))

        assertThat(state(row, context(storage = true)).status)
            .isEqualTo(SyncTransferStatus.OutOfStorageSpace)
    }

    @Test
    fun `test that invoke returns Queued when a queued transfer quota error meets only a storage blocker`() {
        val row = row(entry(TransferState.STATE_QUEUED, error = overQuota(1)))

        assertThat(state(row, context(storage = true, transfer = false)).status)
            .isEqualTo(SyncTransferStatus.Queued)
    }

    @Test
    fun `test that invoke returns PausedInQueue when a retrying over quota transfer is under the app wide pause`() {
        val row = row(entry(TransferState.STATE_RETRYING, error = overQuota(1)))

        assertThat(state(row, context(appPaused = true)).status)
            .isEqualTo(SyncTransferStatus.PausedInQueue)
    }

    @Test
    fun `test that invoke returns PausedInQueue when a retrying over quota transfer has an optimistic pause`() {
        val row = row(entry(TransferState.STATE_RETRYING, error = overQuota(0)), optimistic = true)

        assertThat(state(row).status).isEqualTo(SyncTransferStatus.PausedInQueue)
    }

    @Test
    fun `test that invoke returns Retrying when a retrying transfer has an optimistic resume`() {
        val row = row(entry(TransferState.STATE_RETRYING), optimistic = false)

        assertThat(state(row, context(appPaused = true)).status)
            .isEqualTo(SyncTransferStatus.Retrying)
    }

    @Test
    fun `test that invoke returns speed zero when the direction speed is zero`() {
        val row = row(entry(TransferState.STATE_ACTIVE, transferred = 10, speed = 500))

        assertThat(state(row, directionSpeed = 0).speed).isEqualTo(0)
    }

    private fun rowsWithAStatusOtherThanTransferring(): Stream<Arguments> =
        SyncTransferStatus.entries
            .filter { it != SyncTransferStatus.Transferring }
            .map { Arguments.of(it, rowShowing(it)) }
            .stream()

    private fun rowShowing(status: SyncTransferStatus): SyncTransferRow {
        fun moving(state: TransferState, transferred: Long = 1, error: SyncTransferError? = null) =
            row(entry(state, transferred = transferred, total = 1_000, speed = 300, error = error))
        return when (status) {
            SyncTransferStatus.Starting -> moving(TransferState.STATE_ACTIVE, transferred = 0)
            SyncTransferStatus.Transferring -> moving(TransferState.STATE_ACTIVE)
            SyncTransferStatus.Queued -> moving(TransferState.STATE_QUEUED)
            SyncTransferStatus.OutOfStorageSpace ->
                moving(TransferState.STATE_RETRYING, error = overQuota(0))

            SyncTransferStatus.OutOfTransferQuota ->
                moving(TransferState.STATE_RETRYING, error = overQuota(1))

            SyncTransferStatus.Paused -> moving(TransferState.STATE_PAUSED)
            SyncTransferStatus.PausedInQueue -> moving(TransferState.STATE_PAUSED, transferred = 0)
            SyncTransferStatus.Retrying -> moving(TransferState.STATE_RETRYING)
            SyncTransferStatus.Completing -> moving(TransferState.STATE_COMPLETING)
            SyncTransferStatus.Completed -> moving(TransferState.STATE_COMPLETED)
        }
    }

    private fun state(
        row: SyncTransferRow,
        context: SyncTransferRowContext = context(),
        directionSpeed: Long? = null,
    ): SyncTransferRowState = underTest(row, context, directionSpeed)

    private fun entry(
        state: TransferState,
        transferred: Long = 0,
        total: Long = 100,
        speed: Long = 0,
        meanSpeed: Long = 0,
        error: SyncTransferError? = null,
        foreign: Boolean = false,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = SyncTransferEntry(
        tag = 1,
        direction = direction,
        state = state,
        fileName = "a.jpg",
        totalBytes = total,
        transferredBytes = transferred,
        speed = speed,
        meanSpeed = meanSpeed,
        priority = 1,
        notificationNumber = 1,
        lastError = error,
        isForeignOverQuota = foreign,
        path = null,
    )

    private fun row(entry: SyncTransferEntry, optimistic: Boolean? = null) =
        SyncTransferRow(entry, SyncMediaType.Image, optimistic)

    private fun context(
        appPaused: Boolean = false,
        storage: Boolean = false,
        transfer: Boolean = false,
    ) = SyncTransferRowContext(appPaused, storage, transfer)

    private fun overQuota(value: Long) =
        SyncTransferError(SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE, value)
}
