package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Clears the user pause of every sync paused by the user and resumes those a sync condition does
 * not hold back; a held sync resumes when its condition clears. Returns how many syncs it acted on.
 */
internal class ResumeAllSyncsUseCase @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val isSyncPausedByTheUserUseCase: IsSyncPausedByTheUserUseCase,
    private val setUserPausedSyncUseCase: SetUserPausedSyncUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase,
) {
    suspend operator fun invoke(): Int {
        val shouldSync = monitorShouldSyncUseCase().first()
        val syncs = getFolderPairsUseCase().filter { isPausedByTheUser(it.id) }
        withContext(NonCancellable) {
            syncs.forEach { sync ->
                val isCleared = logFailure("clear the user pause of sync ${sync.id}") {
                    setUserPausedSyncUseCase(sync.id, paused = false)
                }
                // A sync resumed with its row still present would be paused for good at the next
                // condition change
                if (isCleared && shouldSync && sync.hasNoError() &&
                    sync.syncStatus == SyncStatus.PAUSED
                ) {
                    logFailure("resume sync ${sync.id}") { resumeSyncUseCase(sync.id) }
                }
            }
        }
        return syncs.size
    }

    private suspend fun isPausedByTheUser(syncId: Long): Boolean = try {
        isSyncPausedByTheUserUseCase(syncId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Could not read the user pause of sync $syncId")
        false
    }
}

private fun FolderPair.hasNoError() = syncError == null || syncError == SyncError.NO_SYNC_ERROR

private suspend fun logFailure(action: String, block: suspend () -> Unit): Boolean = try {
    block()
    true
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Timber.e(e, "Could not $action")
    false
}
