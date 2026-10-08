package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEnginePauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineWork
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncEngineStateUseCaseTest {

    private val underTest = GetSyncEngineStateUseCase()

    private val stalled = SyncEngineSignals(isWaiting = false, isSyncStalled = true)
    private val waiting = SyncEngineSignals(isWaiting = true, isSyncStalled = false)
    private val pausedByUser = SyncProgressPhase.Paused(SyncPauseCause.User)

    @Test
    fun `test that invoke returns Offline when connectivity is lost while a sync is scanning`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, scanning = true)), blockers = blockers(isOffline = true))
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Offline)
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Scanning)
    }

    @Test
    fun `test that invoke returns NotSeeded when the sync list is not seeded`() {
        val result = underTest(inputs(runSnapshotSeeded = false))

        assertThat(result).isEqualTo(SyncEngineState.NotSeeded)
    }

    @Test
    fun `test that invoke returns NotSeeded before the store is seeded`() {
        val result = underTest(inputs(isStoreSeeded = false))

        assertThat(result).isEqualTo(SyncEngineState.NotSeeded)
    }

    @Test
    fun `test that invoke returns NotSeeded while the status is not seeded`() {
        val result = underTest(inputs().copy(status = status(isSeeded = false)))

        assertThat(result).isEqualTo(SyncEngineState.NotSeeded)
    }

    @Test
    fun `test that invoke returns NoSyncs when the phase is no syncs`() {
        val result = underTest(
            inputs(syncs = emptyList(), phase = SyncProgressPhase.NoSyncs)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.NoSyncs)
    }

    @Test
    fun `test that invoke returns IssuesFound when the stall flag is set and issues are listed`() {
        val result = underTest(
            inputs(
                syncs = listOf(running(1, syncing = true)),
                blockers = blockers(stalledIssueCount = 2),
                signals = stalled,
            )
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.IssuesFound(2))
        assertThat(result.issueCount).isEqualTo(2)
    }

    @Test
    fun `test that invoke returns Busy syncing when the stall flag is set before the issues list loads`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, syncing = true)), signals = stalled, pending = 0)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Syncing))
        assertThat(result.issueCount).isEqualTo(0)
    }

    @Test
    fun `test that invoke reports no issues while the stall flag is off even if issues were listed`() {
        val result = underTest(inputs(blockers = blockers(stalledIssueCount = 3)))

        assertThat(result.issueCount).isEqualTo(0)
        assertThat(result.status).isNotInstanceOf(SyncEngineStatus.IssuesFound::class.java)
    }

    @Test
    fun `test that invoke ranks offline above issues found`() {
        val result = underTest(
            inputs(
                blockers = blockers(isOffline = true, stalledIssueCount = 1),
                signals = stalled,
            )
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Offline)
        assertThat(result.issueCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke ranks offline above no syncs`() {
        val result = underTest(
            inputs(
                syncs = emptyList(),
                phase = SyncProgressPhase.NoSyncs,
                blockers = blockers(isOffline = true),
            )
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Offline)
    }

    @Test
    fun `test that invoke ranks issues found above the app wide pause`() {
        val result = underTest(
            inputs(
                blockers = blockers(areTransfersPaused = true, stalledIssueCount = 1),
                signals = stalled,
            )
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.IssuesFound(1))
    }

    @Test
    fun `test that invoke returns Paused when all transfers are paused while a sync is scanning`() {
        val result = underTest(
            inputs(
                syncs = listOf(running(1, scanning = true)),
                blockers = blockers(areTransfersPaused = true),
            )
        )

        assertThat(result.status)
            .isEqualTo(SyncEngineStatus.Paused(SyncEnginePauseCause.TransfersPaused))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Scanning)
    }

    @Test
    fun `test that invoke ranks the app wide pause above the syncs pause`() {
        val result = underTest(
            inputs(
                syncs = listOf(suspended(1), suspended(2)),
                phase = pausedByUser,
                blockers = blockers(areTransfersPaused = true),
            )
        )

        assertThat(result.status)
            .isEqualTo(SyncEngineStatus.Paused(SyncEnginePauseCause.TransfersPaused))
    }

    @Test
    fun `test that invoke returns Paused with the user cause after pause all`() {
        val result = underTest(
            inputs(syncs = listOf(suspended(1), suspended(2)), phase = pausedByUser, pending = 0)
        )

        assertThat(result.status).isEqualTo(
            SyncEngineStatus.Paused(SyncEnginePauseCause.Syncs(SyncPauseCause.User))
        )
        assertThat(result.busyWork).isNull()
    }

    @Test
    fun `test that invoke keeps the busy work while pause all cancels the last sync rows`() {
        val result = underTest(
            inputs(syncs = listOf(suspended(1), suspended(2)), phase = pausedByUser, pending = 3)
        )

        assertThat(result.status).isEqualTo(
            SyncEngineStatus.Paused(SyncEnginePauseCause.Syncs(SyncPauseCause.User))
        )
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Transferring)
    }

    @Test
    fun `test that invoke returns Paused with the user cause at the first seeded state after a restart with every sync paused`() {
        val result = underTest(
            inputs(
                syncs = listOf(suspended(1), suspended(2)),
                phase = pausedByUser,
                isStoreSeeded = true,
                pending = 0,
                signals = SyncEngineSignals.NotWaiting,
            )
        )

        assertThat(result.status).isEqualTo(
            SyncEngineStatus.Paused(SyncEnginePauseCause.Syncs(SyncPauseCause.User))
        )
        assertThat(result.busyWork).isNull()
        assertThat(result.reevaluateAtMillis).isNull()
    }

    @Test
    fun `test that invoke returns Paused with the syncs cause when the phase is paused`() {
        val cause = SyncPauseCause.Condition(SyncPauseReason.NotCharging)

        val result = underTest(
            inputs(
                syncs = listOf(suspended(1), suspended(2)),
                phase = SyncProgressPhase.Paused(cause),
            )
        )

        assertThat(result.status)
            .isEqualTo(SyncEngineStatus.Paused(SyncEnginePauseCause.Syncs(cause)))
    }

    @Test
    fun `test that invoke returns Busy syncing when every pending sync row is paused one by one`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, syncing = true)), pending = 4)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Syncing))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Syncing)
    }

    @Test
    fun `test that invoke returns Busy scanning when a running sync reports isScanning`() {
        val result = underTest(inputs(syncs = listOf(running(1, scanning = true))))

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pendingOrLoadingSyncs")
    fun `test that invoke returns Busy scanning when a sync is pending or loading`(
        sync: SyncWorkState,
    ) {
        val result = underTest(inputs(syncs = listOf(sync)))

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
    }

    private fun pendingOrLoadingSyncs(): Stream<Arguments> = Stream.of(
        Arguments.of(pending(1)),
        Arguments.of(loading(1)),
    )

    @Test
    fun `test that invoke returns Busy scanning while a running sync awaits its first stats`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, hasStats = false, since = 97_000)), now = 100_000)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
        assertThat(result.reevaluateAtMillis).isEqualTo(107_000)
    }

    @Test
    fun `test that invoke stops counting a sync without stats as scanning after the cap`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, hasStats = false, since = 90_000)), now = 100_000)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
        assertThat(result.reevaluateAtMillis).isNull()
    }

    @Test
    fun `test that invoke returns reevaluateAtMillis while a sync awaits its first stats`() {
        val result = underTest(
            inputs(
                syncs = listOf(
                    running(1, hasStats = false, since = 97_000),
                    running(2, hasStats = false, since = 95_000),
                ),
                now = 100_000,
            )
        )

        assertThat(result.reevaluateAtMillis).isEqualTo(105_000)
    }

    @Test
    fun `test that invoke returns no reevaluateAtMillis when no sync awaits its first stats`() {
        val result = underTest(
            inputs(syncs = listOf(running(1), running(2, syncing = true)), pending = 1)
        )

        assertThat(result.reevaluateAtMillis).isNull()
    }

    @Test
    fun `test that invoke ignores stats of a suspended sync`() {
        val staleScanning = SyncWorkState(
            syncId = 1,
            runState = SyncRunState.Suspended,
            isStorageOverQuota = false,
            stats = SyncStatsSample(
                isScanning = true,
                isSyncing = true,
                pendingUploads = 0,
                pendingDownloads = 0,
            ),
            runningSinceMillis = null,
        )

        val result = underTest(inputs(syncs = listOf(staleScanning)))

        assertThat(result.status).isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
    }

    @Test
    fun `test that invoke ignores stats of a sync with an error`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, scanning = true, hasError = true)))
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
    }

    @Test
    fun `test that invoke returns UpToDate when isSyncing is set with no pending sync transfer and no stall`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, syncing = true)), pending = 0)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
        assertThat(result.busyWork).isNull()
    }

    @Test
    fun `test that invoke returns Busy syncing when isSyncing is set and a sync transfer is pending`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, syncing = true)), pending = 3)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Syncing))
    }

    @Test
    fun `test that invoke returns Busy syncing rather than waiting while the sdk retries with sync transfers pending`() {
        val result = underTest(
            inputs(syncs = listOf(running(1, syncing = true)), pending = 3, signals = waiting)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Syncing))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Syncing)
    }

    @Test
    fun `test that invoke returns Busy waiting when the sdk reports a retry reason`() {
        val result = underTest(
            inputs(syncs = listOf(running(1)), pending = 0, signals = waiting)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Waiting))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Waiting)
    }

    @Test
    fun `test that invoke returns Busy waiting when the stall flag is set and no sync is syncing`() {
        val result = underTest(
            inputs(syncs = listOf(running(1)), pending = 0, signals = stalled)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Waiting))
    }

    @Test
    fun `test that invoke reports waiting as the busy work behind the app wide pause`() {
        val result = underTest(
            inputs(
                syncs = listOf(running(1)),
                blockers = blockers(areTransfersPaused = true),
                pending = 0,
                signals = waiting,
            )
        )

        assertThat(result.status)
            .isEqualTo(SyncEngineStatus.Paused(SyncEnginePauseCause.TransfersPaused))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Waiting)
    }

    @Test
    fun `test that invoke returns Busy transferring when a sync transfer is pending and no sync is syncing`() {
        val result = underTest(inputs(syncs = listOf(running(1)), pending = 2))

        assertThat(result.status)
            .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Transferring))
    }

    @Test
    fun `test that invoke prefers scanning over syncing waiting and transferring`() {
        val result = underTest(
            inputs(
                syncs = listOf(running(1, scanning = true), running(2, syncing = true)),
                pending = 2,
                signals = waiting,
            )
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
    }

    @Test
    fun `test that invoke keeps the busy work when issues are found`() {
        val result = underTest(
            inputs(
                syncs = listOf(running(1, syncing = true)),
                blockers = blockers(stalledIssueCount = 1),
                signals = stalled,
            )
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.IssuesFound(1))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Syncing)
    }

    @Test
    fun `test that invoke keeps the busy work when paused`() {
        val result = underTest(
            inputs(blockers = blockers(areTransfersPaused = true), pending = 2)
        )

        assertThat(result.status)
            .isEqualTo(SyncEngineStatus.Paused(SyncEnginePauseCause.TransfersPaused))
        assertThat(result.busyWork).isEqualTo(SyncEngineWork.Transferring)
    }

    @Test
    fun `test that invoke returns UpToDate with account full when storage is over quota and nothing runs`() {
        val result = underTest(inputs(blockers = blockers(isStorageOverQuota = true)))

        assertThat(result.status).isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = true))
        assertThat(result.busyWork).isNull()
    }

    @Test
    fun `test that invoke counts syncs disabled by an error`() {
        val result = underTest(
            inputs(
                syncs = listOf(
                    suspended(1, hasError = true),
                    disabled(2, hasError = true),
                    suspended(3),
                    running(4, hasError = true),
                ),
            )
        )

        assertThat(result.disabledSyncCount).isEqualTo(2)
    }

    @Test
    fun `test that invoke never labels disabled syncs`() {
        val result = underTest(
            inputs(syncs = listOf(disabled(1, hasError = true)), phase = SyncProgressPhase.UpToDate)
        )

        assertThat(result.status).isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
        assertThat(result.disabledSyncCount).isEqualTo(1)
    }

    private fun inputs(
        syncs: List<SyncWorkState> = listOf(running(1)),
        phase: SyncProgressPhase = SyncProgressPhase.UpToDate,
        blockers: SyncProgressBlockers = blockers(),
        signals: SyncEngineSignals = SyncEngineSignals.NotWaiting,
        isStoreSeeded: Boolean = true,
        pending: Int = 0,
        now: Long = 100_000L,
        runSnapshotSeeded: Boolean = true,
    ) = GetSyncEngineStateUseCase.Inputs(
        runSnapshot = SyncRunSnapshot(
            isSeeded = runSnapshotSeeded,
            syncs = syncs,
            progress = SyncEpisodeProgress.None,
        ),
        status = status(phase = phase),
        blockers = blockers,
        signals = signals,
        isStoreSeeded = isStoreSeeded,
        pendingSyncTransfers = pending,
        nowMillis = now,
    )

    private fun status(
        phase: SyncProgressPhase = SyncProgressPhase.UpToDate,
        isSeeded: Boolean = true,
    ) = SyncProgressStatus(isSeeded = isSeeded, phase = phase, progress = SyncEpisodeProgress.None)

    private fun blockers(
        isOffline: Boolean = false,
        isStorageOverQuota: Boolean = false,
        isTransferOverQuota: Boolean = false,
        areTransfersPaused: Boolean = false,
        stalledIssueCount: Int = 0,
    ) = SyncProgressBlockers(
        isOffline = isOffline,
        isStorageOverQuota = isStorageOverQuota,
        isTransferOverQuota = isTransferOverQuota,
        areTransfersPaused = areTransfersPaused,
        stalledIssueCount = stalledIssueCount,
    )

    private fun running(
        id: Long,
        scanning: Boolean = false,
        syncing: Boolean = false,
        hasError: Boolean = false,
        since: Long = 0L,
        hasStats: Boolean = true,
    ) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = if (hasStats) SyncStatsSample(scanning, syncing, 0, 0) else null,
        runningSinceMillis = since,
        hasError = hasError,
    )

    private fun suspended(id: Long, hasError: Boolean = false) =
        inState(id, SyncRunState.Suspended, hasError)

    private fun disabled(id: Long, hasError: Boolean = false) =
        inState(id, SyncRunState.Disabled, hasError)

    private fun pending(id: Long) = inState(id, SyncRunState.Pending)

    private fun loading(id: Long) = inState(id, SyncRunState.Loading)

    private fun inState(id: Long, state: SyncRunState, hasError: Boolean = false) = SyncWorkState(
        syncId = id,
        runState = state,
        isStorageOverQuota = false,
        stats = null,
        runningSinceMillis = null,
        hasError = hasError,
    )
}
