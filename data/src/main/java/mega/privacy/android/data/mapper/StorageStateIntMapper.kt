package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiJava

/**
 * Mapper to convert Mega [StorageState] to [Int]
 */
typealias StorageStateIntMapper = (@JvmSuppressWildcards StorageState) -> @JvmSuppressWildcards Int

/**
 * Map [StorageState] to [Int]
 */
internal fun storageStateToInt(storageState: StorageState) = when (storageState) {
    StorageState.Unknown -> MegaApiJava.STORAGE_STATE_UNKNOWN
    StorageState.Green -> MegaApiJava.STORAGE_STATE_GREEN
    StorageState.Orange -> MegaApiJava.STORAGE_STATE_ORANGE
    StorageState.Red -> MegaApiJava.STORAGE_STATE_RED
    StorageState.Change -> MegaApiJava.STORAGE_STATE_CHANGE
    StorageState.PayWall -> MegaApiJava.STORAGE_STATE_PAYWALL
}