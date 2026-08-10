package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import javax.inject.Inject

/**
 * Use case to resume syncs that were suspended by the SDK because the account ran out of
 * storage quota, once the storage state is back to normal (e.g. after upgrading to a Pro plan).
 *
 * Syncs explicitly paused by the user are left untouched.
 */
internal class ResumeSyncsSuspendedByStorageOverquotaUseCase @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val isSyncPausedByTheUserUseCase: IsSyncPausedByTheUserUseCase,
) {

    suspend operator fun invoke() {
        getFolderPairsUseCase()
            .filter {
                it.syncError == SyncError.STORAGE_OVERQUOTA && !isSyncPausedByTheUserUseCase(it.id)
            }
            .forEach { resumeSyncUseCase(it.id) }
    }
}
