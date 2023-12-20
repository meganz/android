package mega.privacy.android.feature.sync.data.model

import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats

/**
 * Event emitted by the Mega SDK when a sync event occurs
 */
sealed interface MegaSyncListenerEvent {

    /**
     * OnSyncDeleted
     *
     * @brief This callback will be called when a sync is removed.
     *
     * This entail that the sync is completely removed from cache
     *
     * The SDK retains the ownership of the sync parameter.
     * Don't use it after this functions returns.
     *
     * @param sync MegaSync object representing a sync
     */
    data class OnSyncDeleted(val sync: MegaSync) : MegaSyncListenerEvent

    /**
     * OnSyncStatsUpdated
     *
     * @brief This function is called when there is an update on
     * the number of nodes or transfers in the sync
     *
     * The SDK retains the ownership of the MegaSyncStats.
     * Don't use it after this functions returns. But you can copy it
     *
     * @param syncStats Identifies the sync and provides the counts
     */
    data class OnSyncStatsUpdated(val syncStats: MegaSyncStats) : MegaSyncListenerEvent

    /**
     * OnSyncStateChanged
     *
     * @brief This function is called when the state of the synchronization changes
     *
     * The SDK calls this function when the state of the synchronization changes. you can use
     * MegaSync::getRunState to get the new state of the synchronization
     * and MegaSync::getError to get the error if any.
     *
     * The SDK retains the ownership of the sync parameter.
     * Don't use it after this functions returns.
     *
     * @param sync MegaSync object that has changed its state
     */
    data class OnSyncStateChanged(val sync: MegaSync) : MegaSyncListenerEvent

    /**
     * OnGlobalSyncStateChanged
     *
     * @brief This function is called with the state of the synchronization engine has changed
     *
     * You can call MegaApi::isScanning and MegaApi::isWaiting to know the global state
     * of the synchronization engine.
     */
    data object OnGlobalSyncStateChanged : MegaSyncListenerEvent

    /**
     * OnRefreshSyncState
     *
     * @brief This function is called with the state of the synchronization engine has changed
     *
     * You can call MegaApi::isScanning and MegaApi::isWaiting to know the global state
     * of the synchronization engine.
     */
    data object OnRefreshSyncState : MegaSyncListenerEvent
}
