package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SyncRecordType

/**
 * Mapper to convert related integer values in database to [SyncRecordType]
 */
typealias SyncRecordTypeMapper = (@JvmSuppressWildcards Int) -> @JvmSuppressWildcards SyncRecordType?

/**
 * Map [Int] to [SyncRecordType]
 */
internal fun toSyncRecordType(value: Int): SyncRecordType? =
    when (value) {
        1 -> SyncRecordType.TYPE_PHOTO
        2 -> SyncRecordType.TYPE_VIDEO
        -1 -> SyncRecordType.TYPE_ANY
        else -> null
    }
