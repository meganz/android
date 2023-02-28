package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType
import javax.inject.Inject

/**
 * Default implementation of [SyncRecordTypeMapper]
 */
internal class SyncRecordTypeMapperImpl @Inject constructor() : SyncRecordTypeMapper {
    override fun invoke(value: Int): SyncRecordType? = when (value) {
        1 -> SyncRecordType.TYPE_PHOTO
        2 -> SyncRecordType.TYPE_VIDEO
        -1 -> SyncRecordType.TYPE_ANY
        else -> null
    }
}