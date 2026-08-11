package mega.privacy.android.feature.sync.domain.usecase.sync.option

import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

/**
 * Use case for setting if sync should be paused when the device is in power save mode (Battery Saver)
 */
internal class SetPauseSyncOnBatterySaverUseCase @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository,
) {

    suspend operator fun invoke(checked: Boolean) {
        syncPreferencesRepository.setPauseSyncOnBatterySaver(checked)
    }
}
