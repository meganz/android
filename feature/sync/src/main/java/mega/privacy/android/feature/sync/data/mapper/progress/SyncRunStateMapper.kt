package mega.privacy.android.feature.sync.data.mapper.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import nz.mega.sdk.MegaSync
import javax.inject.Inject

/**
 * Maps the SDK run state of a sync. An unknown value is Disabled: it is never work and never
 * counts as an interruption.
 */
internal class SyncRunStateMapper @Inject constructor() {

    operator fun invoke(runState: Int): SyncRunState = when (runState) {
        MegaSync.SyncRunningState.RUNSTATE_PENDING.swigValue() -> SyncRunState.Pending
        MegaSync.SyncRunningState.RUNSTATE_LOADING.swigValue() -> SyncRunState.Loading
        MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue() -> SyncRunState.Running
        MegaSync.SyncRunningState.RUNSTATE_SUSPENDED.swigValue() -> SyncRunState.Suspended
        else -> SyncRunState.Disabled
    }
}
