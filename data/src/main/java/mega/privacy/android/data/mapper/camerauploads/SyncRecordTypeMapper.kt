package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType

/**
 * Mapper that converts related [Int] values in the Database to [SyncRecordType]
 */
internal fun interface SyncRecordTypeMapper {

    /**
     * Invocation function
     *
     * @param value [Int] from Database
     *
     * @return The corresponding [SyncRecordType], or null if there is no match
     */
    operator fun invoke(value: Int): SyncRecordType?
}