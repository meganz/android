package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseToggle
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import javax.inject.Inject

/** Returns the pause action to offer, from the syncs' run states, errors and user pause rows. */
internal class GetSyncPauseToggleUseCase @Inject constructor() {

    /** ResumeAll when every sync without an error is paused by the user and not active. */
    operator fun invoke(syncs: List<SyncWorkState>, userPausedSyncIds: Set<Long>): SyncPauseToggle {
        // Pause all never pauses a sync with an error, because a suspend erases the error, so
        // such a sync must not keep the toggle on PauseAll.
        val controllable = syncs.filterNot { it.hasError }
        val isHeldByUser = controllable.isNotEmpty() &&
                controllable.all { it.syncId in userPausedSyncIds && it.runState !in activeRunStates }
        return if (isHeldByUser) SyncPauseToggle.ResumeAll else SyncPauseToggle.PauseAll
    }

    private companion object {
        val activeRunStates = setOf(SyncRunState.Pending, SyncRunState.Loading, SyncRunState.Running)
    }
}
