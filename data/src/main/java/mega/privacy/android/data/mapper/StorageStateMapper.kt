package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_CHANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_GREEN
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_ORANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_RED
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_UNKNOWN
import javax.inject.Inject

/**
 * Mapper to map Mega Storage State to [StorageState]
 */
class StorageStateMapper @Inject constructor() {
    /**
     * Convert Mega Storage State to [StorageState]
     *
     * @param storageState Mega Storage State
     * @return             [StorageState]
     */
    operator fun invoke(storageState: Int): StorageState = when (storageState) {
        STORAGE_STATE_UNKNOWN -> StorageState.Unknown
        STORAGE_STATE_GREEN -> StorageState.Green
        STORAGE_STATE_ORANGE -> StorageState.Orange
        STORAGE_STATE_RED -> StorageState.Red
        STORAGE_STATE_CHANGE -> StorageState.Change
        STORAGE_STATE_PAYWALL -> StorageState.PayWall
        else -> StorageState.Unknown
    }
}