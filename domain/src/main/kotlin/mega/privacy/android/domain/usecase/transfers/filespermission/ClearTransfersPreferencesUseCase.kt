package mega.privacy.android.domain.usecase.transfers.filespermission

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to clear transfer preferences.
 */
class ClearTransfersPreferencesUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        transfersRepository.clearPreferences()
    }
}