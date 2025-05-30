package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

class ChangeSyncLocalRootUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(
        folderPairId: Long,
        newLocalPath: String,
    ) = syncRepository.changeSyncLocalRoot(folderPairId, newLocalPath)
}
