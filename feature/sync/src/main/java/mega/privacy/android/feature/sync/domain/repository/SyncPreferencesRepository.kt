package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for storing user preference regarding Sync
 */
interface SyncPreferencesRepository {

    /**
     * Sets if sync should be done only when connected to WiFi
     */
    fun setSyncByWiFi(checked: Boolean)

    /**
     * Gets if sync should be done only when connected to WiFi
     */
    fun monitorSyncByWiFi(): StateFlow<Boolean>
}
