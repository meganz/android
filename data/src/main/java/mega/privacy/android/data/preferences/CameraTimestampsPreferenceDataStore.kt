package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.CameraTimestampsPreferenceGateway
import mega.privacy.android.data.qualifier.CameraTimestampsPreference
import javax.inject.Inject

/**
 * Default Implementation of [CameraTimestampsPreferenceGateway]
 */
class CameraTimestampsPreferenceDataStore
@Inject constructor(@CameraTimestampsPreference private val dataStore: DataStore<Preferences>) :
    CameraTimestampsPreferenceGateway {
    override suspend fun backupTimestampsAndFolderHandle(
        primaryUploadFolderHandle: Long,
        secondaryUploadFolderHandle: Long,
        camSyncTimeStamp: String?,
        camVideoSyncTimeStamp: String?,
        secSyncTimeStamp: String?,
        secVideoSyncTimeStamp: String?,
    ) {
        dataStore.edit {
            it[KEY_CAM_SYNC_TIMESTAMP] = camSyncTimeStamp.orEmpty()
            it[KEY_CAM_VIDEO_SYNC_TIMESTAMP] = camVideoSyncTimeStamp.orEmpty()
            it[KEY_SEC_SYNC_TIMESTAMP] = secSyncTimeStamp.orEmpty()
            it[KEY_SEC_VIDEO_SYNC_TIMESTAMP] = secVideoSyncTimeStamp.orEmpty()
            it[KEY_PRIMARY_HANDLE] = primaryUploadFolderHandle
            it[KEY_SECONDARY_HANDLE] = secondaryUploadFolderHandle
        }
    }

    override suspend fun getPrimaryHandle() =
        dataStore.monitor(KEY_PRIMARY_HANDLE).firstOrNull()

    override suspend fun getSecondaryHandle() =
        dataStore.monitor(KEY_SECONDARY_HANDLE).firstOrNull()

    override suspend fun getPrimaryFolderPhotoSyncTime() =
        dataStore.monitor(KEY_CAM_SYNC_TIMESTAMP).firstOrNull()

    override suspend fun getSecondaryFolderPhotoSyncTime() =
        dataStore.monitor(KEY_SEC_SYNC_TIMESTAMP).firstOrNull()

    override suspend fun getPrimaryFolderVideoSyncTime() =
        dataStore.monitor(KEY_CAM_VIDEO_SYNC_TIMESTAMP).firstOrNull()

    override suspend fun getSecondaryFolderVideoSyncTime() =
        dataStore.monitor(KEY_SEC_VIDEO_SYNC_TIMESTAMP).firstOrNull()

    override suspend fun clearPrimaryCameraSyncRecords() {
        dataStore.edit {
            it[KEY_CAM_SYNC_TIMESTAMP] = ""
            it[KEY_CAM_VIDEO_SYNC_TIMESTAMP] = ""
            it[KEY_PRIMARY_HANDLE] = 0
        }
    }

    override suspend fun clearSecondaryCameraSyncRecords() {
        dataStore.edit {
            it[KEY_SEC_SYNC_TIMESTAMP] = ""
            it[KEY_SEC_VIDEO_SYNC_TIMESTAMP] = ""
            it[KEY_SECONDARY_HANDLE] = 0
        }
    }

    /**
     * Keys for backing up time stamps
     */
    companion object {
        /**
         * String Preference Key for Primary Camera Photo
         */
        val KEY_CAM_SYNC_TIMESTAMP = stringPreferencesKey("KEY_CAM_SYNC_TIMESTAMP")

        /**
         * String Preference Key for Primary Camera Video
         */
        val KEY_CAM_VIDEO_SYNC_TIMESTAMP =
            stringPreferencesKey("KEY_CAM_VIDEO_SYNC_TIMESTAMP")

        /**
         * String Preference Key for Secondary Camera Photo
         */
        val KEY_SEC_SYNC_TIMESTAMP = stringPreferencesKey("KEY_SEC_SYNC_TIMESTAMP")

        /**
         * String Preference Key for Secondary Camera Video
         */
        val KEY_SEC_VIDEO_SYNC_TIMESTAMP =
            stringPreferencesKey("KEY_SEC_VIDEO_SYNC_TIMESTAMP")

        /**
         * Long Preference Key for Primary Folder Handle
         */
        val KEY_PRIMARY_HANDLE = longPreferencesKey("KEY_PRIMARY_HANDLE")

        /**
         * Long Preference Key for Secondary Folder Handle
         */
        val KEY_SECONDARY_HANDLE = longPreferencesKey("KEY_SECONDARY_HANDLE")

        /**
         * DataStore File Name
         */
        const val LAST_CAM_SYNC_TIMESTAMP_FILE = "LAST_CAM_SYNC_TIMESTAMP_FILE"
    }

}
