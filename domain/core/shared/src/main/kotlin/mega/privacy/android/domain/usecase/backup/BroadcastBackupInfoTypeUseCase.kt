package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that broadcast backup info type
 *
 * @property backupRepository [BackupRepository]
 */
class BroadcastBackupInfoTypeUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * invocation function
     *
     * @param backupInfoType [BackupInfoType]
     */
    suspend operator fun invoke(backupInfoType: BackupInfoType) =
        backupRepository.broadCastBackupInfoType(backupInfoType)
}
