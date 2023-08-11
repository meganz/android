package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

internal class SetOnboardingShownUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    suspend operator fun invoke(shown: Boolean) {
        syncPreferencesRepository.setOnboardingShown(shown)
    }
}