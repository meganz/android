package mega.privacy.android.feature.sync.data.mock

import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import javax.inject.Inject

/*
 * Mock class to avoid dependency on real Sync SDK
 */
internal class MegaApiSyncMock @Inject constructor() {

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
     * Mocks method syncFolder from megaapi.h
     * @param backupId id of the sync pair to remove
     */
    fun removeSync(backupId: Long) {
        // Mock
    }
}