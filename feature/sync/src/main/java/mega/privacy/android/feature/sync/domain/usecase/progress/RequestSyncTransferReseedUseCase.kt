package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/** Asks the store to reconcile its active rows with a fresh snapshot of the sync transfers. */
internal class RequestSyncTransferReseedUseCase @Inject constructor(
    private val repository: SyncTransferStoreRepository,
) {
    suspend operator fun invoke() = repository.requestReseed()
}
