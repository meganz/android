package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.FolderPreference

/**
 * Repository for per-folder UI preferences (device-local).
 *
 * The `folderKey` identifies a folder: node-handle-as-string for cloud/shares, file path for offline.
 */
interface FolderPreferenceRepository {

    /**
     * Monitor the preferences stored for a folder.
     *
     * @return a [Flow] of the folder's [FolderPreference], or null if none has been stored
     */
    fun monitorFolderPreference(folderKey: String): Flow<FolderPreference?>

    /**
     * Store a folder's preferences, replacing any existing values.
     *
     * The caller supplies the full [FolderPreference] so no prior read is needed.
     */
    suspend fun setFolderPreference(preference: FolderPreference)

    /**
     * Clear all stored per-folder preferences (called on logout).
     */
    suspend fun clearFolderPreferences()
}
