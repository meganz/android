package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.SyncRecordEntity
import mega.privacy.android.domain.entity.SyncRecord
import javax.inject.Inject

internal class SyncRecordEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper,
) {
    suspend operator fun invoke(syncRecord: SyncRecord) = SyncRecordEntity(
        originalPath = encryptData(syncRecord.localPath),
        newPath = encryptData(syncRecord.newPath),
        originalFingerPrint = encryptData(syncRecord.originFingerprint),
        newFingerprint = encryptData(syncRecord.newFingerprint),
        timestamp = encryptData(syncRecord.timestamp?.toString()),
        fileName = encryptData(syncRecord.fileName),
        nodeHandle = encryptData(syncRecord.nodeHandle?.toString()),
        isCopyOnly = encryptData(syncRecord.isCopyOnly.toString()),
        isSecondary = encryptData(syncRecord.isSecondary.toString()),
        latitude = encryptData(syncRecord.latitude?.toString()),
        longitude = encryptData(syncRecord.longitude?.toString()),
        state = syncRecord.status,
        type = syncRecordTypeIntMapper(syncRecord.type)
    )
}
