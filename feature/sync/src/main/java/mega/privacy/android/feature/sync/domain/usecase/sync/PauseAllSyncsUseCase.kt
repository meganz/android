package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Pauses every sync without an SDK error and marks it paused by the user, so the condition monitor
 * and the storage resume leave it paused. Returns how many syncs it acted on.
 */
internal class PauseAllSyncsUseCase @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val setUserPausedSyncUseCase: SetUserPausedSyncUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
) {
    suspend operator fun invoke(): Int {
        val syncs = getFolderPairsUseCase().filter { it.hasNoError() }
        withContext(NonCancellable) { // leaving the caller must not leave the syncs half paused
            syncs.forEach { sync ->
                logFailure("mark sync ${sync.id} as paused by the user") {
                    setUserPausedSyncUseCase(sync.id, paused = true)
                }
                if (sync.isActive()) {
                    logFailure("pause sync ${sync.id}") { pauseSyncUseCase(sync.id) }
                }
            }
        }
        return syncs.size
    }
}

private fun FolderPair.hasNoError() = syncError == null || syncError == SyncError.NO_SYNC_ERROR

private fun FolderPair.isActive() =
    syncStatus == SyncStatus.SYNCING || syncStatus == SyncStatus.SYNCED

private suspend fun logFailure(action: String, block: suspend () -> Unit): Boolean = try {
    block()
    true
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Timber.e(e, "Could not $action")
    false
}
