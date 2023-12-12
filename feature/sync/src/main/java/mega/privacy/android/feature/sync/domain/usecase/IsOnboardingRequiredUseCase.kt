package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

internal class IsOnboardingRequiredUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val syncRepository: SyncRepository,
) {

    suspend operator fun invoke(): Boolean =
        syncRepository.getFolderPairs()
            .isEmpty() && syncPreferencesRepository.getOnboardingShown() != true
}
