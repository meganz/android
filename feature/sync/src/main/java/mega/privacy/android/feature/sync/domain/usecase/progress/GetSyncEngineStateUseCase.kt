package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncEnginePauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineWork
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import javax.inject.Inject

/**
 * Works out the sync status for the whole app with desktop's status chain: NotSeeded, Offline,
 * NoSyncs, IssuesFound, Paused, then Scanning, Syncing, Waiting, Transferring, then UpToDate.
 */
internal class GetSyncEngineStateUseCase @Inject constructor() {

    /**
     * Returns the app-wide state: the first status in the chain that holds, the busy work
     * behind it, and when a time-based rule should be re-evaluated.
     *
     * @param inputs Everything the chain reads
     */
    operator fun invoke(inputs: Inputs): SyncEngineState = with(inputs) {
        if (!runSnapshot.isSeeded || !status.isSeeded || !isStoreSeeded) return SyncEngineState.NotSeeded
        val cap = GetSyncProgressPhaseUseCase.UNKNOWN_STATS_CAP.inWholeMilliseconds
        val live = runSnapshot.syncs.filter { it.runState == SyncRunState.Running && !it.hasError }
        val awaitingUntil = live.mapNotNull { sync ->
            sync.runningSinceMillis?.takeIf { sync.stats == null }?.plus(cap)
                ?.takeIf { nowMillis < it }
        }
        // The SDK's global scanning flag is not exposed; a sync still loading stands in for it.
        val isScanning = live.any { it.stats?.isScanning == true } ||
                runSnapshot.syncs.any {
                    it.runState == SyncRunState.Pending || it.runState == SyncRunState.Loading
                } ||
                awaitingUntil.isNotEmpty()
        val isSdkSyncing = isScanning || live.any { it.stats?.isSyncing == true }
        // The SDK stays syncing for throttled and clone work that has no transfer; desktop hides it.
        val isSyncing = isSdkSyncing && (signals.isSyncStalled || pendingSyncTransfers > 0)
        val isWaiting = signals.isWaiting || signals.isSyncStalled
        val busy = when {
            isScanning -> SyncEngineWork.Scanning
            isSyncing -> SyncEngineWork.Syncing
            isWaiting -> SyncEngineWork.Waiting
            pendingSyncTransfers > 0 -> SyncEngineWork.Transferring
            else -> null
        }
        val issueCount = if (signals.isSyncStalled) blockers.stalledIssueCount else 0
        val phase = status.phase
        SyncEngineState(
            status = when {
                blockers.isOffline -> SyncEngineStatus.Offline
                phase == SyncProgressPhase.NoSyncs -> SyncEngineStatus.NoSyncs
                issueCount > 0 -> SyncEngineStatus.IssuesFound(issueCount)
                blockers.areTransfersPaused ->
                    SyncEngineStatus.Paused(SyncEnginePauseCause.TransfersPaused)

                phase is SyncProgressPhase.Paused ->
                    SyncEngineStatus.Paused(SyncEnginePauseCause.Syncs(phase.cause))

                busy != null -> SyncEngineStatus.Busy(busy)
                else -> SyncEngineStatus.UpToDate(isAccountFull = blockers.isStorageOverQuota)
            },
            busyWork = busy,
            issueCount = issueCount,
            disabledSyncCount = runSnapshot.syncs.count {
                it.hasError &&
                        (it.runState == SyncRunState.Suspended || it.runState == SyncRunState.Disabled)
            },
            reevaluateAtMillis = awaitingUntil.minOrNull(),
        )
    }

    /**
     * The chain's inputs
     *
     * @property isStoreSeeded Whether the sync transfer store is seeded
     * @property pendingSyncTransfers Pending sync transfers, both directions
     * @property nowMillis The current time
     */
    data class Inputs(
        val runSnapshot: SyncRunSnapshot,
        val status: SyncProgressStatus,
        val blockers: SyncProgressBlockers,
        val signals: SyncEngineSignals,
        val isStoreSeeded: Boolean,
        val pendingSyncTransfers: Int,
        val nowMillis: Long,
    )
}
