package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType
import javax.inject.Inject

/**
 * Mapper that converts [SyncRecordType] objects to their [Int] counterparts in the Database
 */
class SyncRecordTypeIntMapper @Inject constructor() {
    /**
     * invoke
     * @param syncRecordType [SyncRecordType]
     */
    operator fun invoke(syncRecordType: SyncRecordType): Int = when (syncRecordType) {
        SyncRecordType.TYPE_PHOTO -> 1
        SyncRecordType.TYPE_VIDEO -> 2
        SyncRecordType.TYPE_ANY -> -1
    }
}
