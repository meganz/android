package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType
import javax.inject.Inject

/**
 * Default implementation of [SyncRecordTypeIntMapper]
 */
internal class SyncRecordTypeIntMapperImpl @Inject constructor() : SyncRecordTypeIntMapper {
    override fun invoke(syncRecordType: SyncRecordType): Int = when (syncRecordType) {
        SyncRecordType.TYPE_PHOTO -> 1
        SyncRecordType.TYPE_VIDEO -> 2
        SyncRecordType.TYPE_ANY -> -1
    }
}