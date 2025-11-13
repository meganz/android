package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.backup.BackupInfo
import nz.mega.sdk.MegaBackupInfoList
import javax.inject.Inject

/**
 * Mapper that converts a [MegaBackupInfoList] into a list of [BackupInfo] objects
 *
 * @property backupInfoMapper [BackupInfoMapper]
 */
internal class BackupInfoListMapper @Inject constructor(
    private val backupInfoMapper: BackupInfoMapper,
) {
    /**
     * Invocation function
     *
     * @param sdkBackupInfoList A potentially nullable [MegaBackupInfoList]
     * @return A list of [BackupInfo] objects
     */
    suspend operator fun invoke(sdkBackupInfoList: MegaBackupInfoList?): List<BackupInfo> {
        return sdkBackupInfoList?.let { backupInfoList ->
            val backupSize = backupInfoList.size()
            if (backupSize <= 0) emptyList()
            else (0 until backupSize).mapNotNull { index ->
                backupInfoMapper(backupInfoList.get(index))
            }
                .sortedByDescending { it.timestamp }
                .distinctBy { "${it.deviceId}:${it.rootHandle.longValue}" }
        } ?: emptyList()
    }
}
