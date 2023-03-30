package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to set master key exported
 */
class SetMasterKeyExportedUseCase @Inject constructor(
    private val repository: SettingsRepository
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = repository.setMasterKeyExported()
}