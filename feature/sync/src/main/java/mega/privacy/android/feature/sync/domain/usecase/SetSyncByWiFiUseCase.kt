package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case for setting if sync should be done only when connected to WiFi
 */
internal class SetSyncByWiFiUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    suspend operator fun invoke(checked: Boolean) {
        syncPreferencesRepository.setSyncByWiFi(checked)
    }
}
