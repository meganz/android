package mega.privacy.android.feature.sync.data.mock

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

/*
 * Mock class to avoid dependency on real Sync SDK
 */
internal class MegaApiSyncMock @Inject constructor() {

    /**
     * Mocks [mega.privacy.android.data.model.GlobalUpdate]
     */
    sealed class GlobalUpdate {
        data class OnGlobalSyncStateChanged(val megaSync: MegaSync) : GlobalUpdate()
    }

    /**
     * Mocks method getSyncs from megaapi.h
     * @return empty list of sync pairs
     */
    val syncs = MegaSyncList()

    /**
     * Mocks method syncFolder from megaapi.h
     */
    fun syncFolder(
        typeTwoway: Any,
        localPath: String,
        nothing: Void?,
        remoteFolderId: Long,
        nothing1: Void?,
        syncStateListener: OptionalMegaRequestListenerInterface,
    ) {
        // Mock
    }

    /**
     * Mocks method removeFolder from megaapi.h
     * @param backupId id of the sync pair to remove
     */
    fun removeSync(backupId: Long) {
        // Mock
    }

    /**
     * Mocks the usage of the setSyncRunState from megaapi.h
     * In real implementation we would call
     * setSyncRunState(sync.getBackupId(), RUNSTATE_RUNNING)
     * for every folder pair
     */
    fun resumeAllSyncs() {
        // Mock
    }

    /**
     * Mocks the usage of the setSyncRunState from megaapi.h
     * In real implementation we would call
     * setSyncRunState(sync.getBackupId(), RUNSTATE_PAUSED)
     * for every folder pair
     */
    fun pauseAllSyncs() {
        // Mock
    }

    /**
     * Mocks
     * [mega.privacy.android.data.gateway.api.MegaApiGateway.removeRequestListener]
     *
     * @param listener
     */
    fun removeRequestListener(listener: MegaRequestListenerInterface) {
        // Mock
    }

    /**
     * Mocks field globalUpdates from [mega.privacy.android.data.facade.MegaApiFacade]
     */
    val globalUpdates: Flow<GlobalUpdate> = callbackFlow {}
}