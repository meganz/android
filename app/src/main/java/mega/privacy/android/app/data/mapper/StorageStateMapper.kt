package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_CHANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_GREEN
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_ORANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_RED
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_UNKNOWN

/**
 * Mapper to map Mega Storage State to [StorageState]
 */
typealias StorageStateMapper = (@JvmSuppressWildcards Int) -> @JvmSuppressWildcards StorageState

internal fun toStorageState(storageState: Int) = when (storageState) {
    STORAGE_STATE_UNKNOWN -> StorageState.Unknown
    STORAGE_STATE_GREEN -> StorageState.Green
    STORAGE_STATE_ORANGE -> StorageState.Orange
    STORAGE_STATE_RED -> StorageState.Red
    STORAGE_STATE_CHANGE -> StorageState.Change
    STORAGE_STATE_PAYWALL -> StorageState.PayWall
    else -> StorageState.Unknown
}