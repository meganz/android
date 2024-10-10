package mega.privacy.android.domain.usecase.transfers.filespermission

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to monitor if the request for files permission is denied.
 */
class MonitorRequestFilesPermissionDeniedUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {

    /**
     * Invoke.
     */
    operator fun invoke() = transfersRepository.monitorRequestFilesPermissionDenied()
}