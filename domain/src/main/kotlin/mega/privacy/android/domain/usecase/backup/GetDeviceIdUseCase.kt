package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Device ID of the current Device
 *
 * @property backupRepository [BackupRepository]
 */
class GetDeviceIdUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * Invocation function
     *
     * @return The potentially nullable Device ID of the Current Device
     */
    suspend operator fun invoke(): String? = backupRepository.getDeviceId()
}