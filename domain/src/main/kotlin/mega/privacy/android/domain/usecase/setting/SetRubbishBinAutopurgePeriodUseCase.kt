package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for setting the autopurge period for the rubbish bin.
 *
 */
class SetRubbishBinAutopurgePeriodUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Sets the autopurge period for the rubbish bin.
     *
     * @param days The number of days after which the rubbish bin will be purged.
     */
    suspend operator fun invoke(days: Int) {
        settingsRepository.setRubbishBinAutopurgePeriod(days)
    }
}