package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

internal class GetSyncStalledIssuesUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(): List<StalledIssue> =
        syncRepository.getSyncStalledIssues()
}