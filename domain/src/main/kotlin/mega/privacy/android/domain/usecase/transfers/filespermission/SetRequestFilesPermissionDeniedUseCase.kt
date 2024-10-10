package mega.privacy.android.domain.usecase.transfers.filespermission

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to set when the request for files permission has been denied.
 */
class SetRequestFilesPermissionDeniedUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        transfersRepository.setRequestFilesPermissionDenied()
    }
}