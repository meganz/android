package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to get export master key
 */
class GetExportMasterKeyUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    /**
     * Invoke
     * @return master key as [String]
     */
    suspend operator fun invoke(): String? = repository.getExportMasterKey()
}