package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that monitors backup info type
 *
 * @property backupRepository [BackupRepository]
 */
class MonitorBackupInfoTypeUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * invocation function
     *
     * @return Flow of [BackupInfoType]
     */
    operator fun invoke() = backupRepository.monitorBackupInfoType()
}
