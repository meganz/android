package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType

/**
 * Mapper that converts [SyncRecordType] objects to their [Int] counterparts in the Database
 */
fun interface SyncRecordTypeIntMapper {

    /**
     * Invocation function
     *
     * @param syncRecordType The [SyncRecordType] to be converted
     *
     * @return The corresponding [Int] value of the [SyncRecordType]
     */
    operator fun invoke(syncRecordType: SyncRecordType): Int
}