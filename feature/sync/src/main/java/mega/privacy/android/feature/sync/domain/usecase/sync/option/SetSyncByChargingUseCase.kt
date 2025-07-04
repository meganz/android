package mega.privacy.android.feature.sync.domain.usecase.sync.option

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case for setting if sync should be done only when charging
 */
internal class SetSyncByChargingUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    suspend operator fun invoke(checked: Boolean) {
        syncPreferencesRepository.setSyncByCharging(checked)
    }
}
