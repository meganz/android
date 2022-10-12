package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SyncRecordType

/**
 * Mapper to convert [SyncRecordType] to related integer values in database
 */
typealias SyncRecordTypeIntMapper = (@JvmSuppressWildcards SyncRecordType) -> @JvmSuppressWildcards Int

/**
 * Map [SyncRecordType] to [Int]
 */
internal fun toSyncRecordTypeInt(recordType: SyncRecordType): Int =
    when (recordType) {
        SyncRecordType.TYPE_PHOTO -> 1
        SyncRecordType.TYPE_VIDEO -> 2
        SyncRecordType.TYPE_ANY -> -1
    }
