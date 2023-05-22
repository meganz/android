package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType
import javax.inject.Inject

/**
 * Mapper that converts related [Int] values in the Database to [SyncRecordType]
 */
class SyncRecordTypeMapper @Inject constructor() {


    /**
     * invoke
     * @param value
     */
    operator fun invoke(value: Int): SyncRecordType = when (value) {
        1 -> SyncRecordType.TYPE_PHOTO
        2 -> SyncRecordType.TYPE_VIDEO
        else -> SyncRecordType.TYPE_ANY
    }
}
