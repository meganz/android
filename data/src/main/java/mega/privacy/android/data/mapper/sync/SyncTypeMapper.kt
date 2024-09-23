package mega.privacy.android.data.mapper.sync

import mega.privacy.android.domain.entity.sync.SyncType
import nz.mega.sdk.MegaSync
import javax.inject.Inject

/**
 * Mapper that converts between [SyncType] and [nz.mega.sdk.MegaSync.SyncType] values
 */
class SyncTypeMapper @Inject constructor() {

    /**
     * Converts the [SyncType] value into the corresponding [nz.mega.sdk.MegaSync.SyncType] value
     *
     * @param syncType The [SyncType] value
     * @return The corresponding [nz.mega.sdk.MegaSync.SyncType] value
     */
    operator fun invoke(syncType: SyncType): MegaSync.SyncType = when (syncType) {
        SyncType.TYPE_TWOWAY -> MegaSync.SyncType.TYPE_TWOWAY
        SyncType.TYPE_BACKUP -> MegaSync.SyncType.TYPE_BACKUP
        else -> MegaSync.SyncType.TYPE_UNKNOWN
    }

    /**
     * Converts the [nz.mega.sdk.MegaSync.SyncType] value into the corresponding [SyncType] value
     *
     * @param syncType The [SyncType] value
     * @return The corresponding [nz.mega.sdk.MegaSync.SyncType] value
     */
    operator fun invoke(syncType: MegaSync.SyncType): SyncType = when (syncType) {
        MegaSync.SyncType.TYPE_TWOWAY -> SyncType.TYPE_TWOWAY
        MegaSync.SyncType.TYPE_BACKUP -> SyncType.TYPE_BACKUP
        else -> SyncType.TYPE_UNKNOWN
    }
}