package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.SyncRecordEntity
import mega.privacy.android.domain.entity.SyncRecord
import javax.inject.Inject

internal class SyncRecordModelMapper @Inject constructor(
    private val decryptData: DecryptData,
    private val syncRecordTypeMapper: SyncRecordTypeMapper,
) {
    suspend operator fun invoke(syncRecordEntity: SyncRecordEntity) = SyncRecord(
        id = syncRecordEntity.id ?: -1,
        localPath = decryptData(syncRecordEntity.originalPath) ?: "",
        newPath = decryptData(syncRecordEntity.newPath),
        originFingerprint = decryptData(syncRecordEntity.originalFingerPrint),
        newFingerprint = decryptData(syncRecordEntity.newFingerprint),
        timestamp = decryptData(syncRecordEntity.timestamp)?.toLong() ?: 0L,
        fileName = decryptData(syncRecordEntity.fileName) ?: "",
        longitude = decryptData(syncRecordEntity.longitude)?.toDoubleOrNull(),
        latitude = decryptData(syncRecordEntity.latitude)?.toDoubleOrNull(),
        status = syncRecordEntity.state ?: -1,
        type = syncRecordTypeMapper(syncRecordEntity.type ?: -1),
        nodeHandle = decryptData(syncRecordEntity.nodeHandle)?.toLongOrNull(),
        isCopyOnly = decryptData(syncRecordEntity.isCopyOnly)?.toBoolean() ?: false,
        isSecondary = decryptData(syncRecordEntity.isSecondary)?.toBoolean() ?: false,
    )
}
