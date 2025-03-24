package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for getting the autopurge period for the rubbish bin.
 *
 */
class GetRubbishBinAutopurgePeriodUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Gets the autopurge period for the rubbish bin.
     *
     * @return The number of days after which the rubbish bin will be purged.
     */
    suspend operator fun invoke(): Int =
        settingsRepository.getRubbishBinAutopurgePeriod()
}