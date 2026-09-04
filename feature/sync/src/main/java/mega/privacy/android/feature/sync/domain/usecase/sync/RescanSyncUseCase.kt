package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case to force a running sync to re-read its filters and walk its local root again.
 *
 * @param syncRepository [SyncRepository]
 */
internal class RescanSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    /**
     * Rescans the sync identified by [syncId].
     *
     * @param syncId the id of the folder pair to rescan. An invalid handle (-1) makes the SDK
     * rescan every sync on the device, so callers must pass a resolved id.
     * @param reFingerprint whether local files are fingerprinted again, which catches content
     * changes that mtime alone does not reveal. Defaults to true so that a rescan prompted by an
     * exclusion change does not also have to trust the sync's existing view of the local tree.
     */
    suspend operator fun invoke(syncId: Long, reFingerprint: Boolean = true) {
        syncRepository.rescanSync(syncId, reFingerprint)
    }
}
