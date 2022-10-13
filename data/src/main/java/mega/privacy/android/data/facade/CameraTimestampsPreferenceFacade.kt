package mega.privacy.android.data.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.preferences.CameraTimestampsPreferenceGateway
import javax.inject.Inject

/**
 * Default implementation of [CameraTimestampsPreferenceGateway]
 */
class CameraTimestampsPreferenceFacade @Inject constructor(@ApplicationContext private val context: Context) :
    CameraTimestampsPreferenceGateway {

    override suspend fun backupTimestampsAndFolderHandle(
        primaryUploadFolderHandle: Long,
        secondaryUploadFolderHandle: Long,
        camSyncTimeStamp: String?,
        camVideoSyncTimeStamp: String?,
        secSyncTimeStamp: String?,
        secVideoSyncTimeStamp: String?,
    ) = context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
        Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CAM_SYNC_TIMESTAMP, camSyncTimeStamp)
        .putString(KEY_CAM_VIDEO_SYNC_TIMESTAMP,
            camVideoSyncTimeStamp)
        .putString(KEY_SEC_SYNC_TIMESTAMP, secSyncTimeStamp)
        .putString(KEY_SEC_VIDEO_SYNC_TIMESTAMP,
            secVideoSyncTimeStamp)
        .putLong(KEY_PRIMARY_HANDLE, primaryUploadFolderHandle)
        .putLong(KEY_SECONDARY_HANDLE, secondaryUploadFolderHandle)
        .apply()

    override suspend fun getPrimaryHandle(): Long? =
        // todo remove takeIf when changed to Datastore to support nullable value
        context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
            Context.MODE_PRIVATE).getLong(KEY_PRIMARY_HANDLE, -2).takeIf { it != -2L }

    override suspend fun getSecondaryHandle(): Long? =
        // todo remove takeIf when changed to Datastore to support nullable value
        context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
            Context.MODE_PRIVATE).getLong(KEY_SECONDARY_HANDLE, -2L).takeIf { it != -2L }

    override suspend fun getPrimaryFolderPhotoSyncTime(): String? =
        context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
            Context.MODE_PRIVATE).getString(KEY_CAM_SYNC_TIMESTAMP, null)

    override suspend fun getSecondaryFolderPhotoSyncTime(): String? =
        context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
            Context.MODE_PRIVATE).getString(KEY_SEC_SYNC_TIMESTAMP, null)

    override suspend fun getPrimaryFolderVideoSyncTime(): String? =
        context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
            Context.MODE_PRIVATE).getString(KEY_CAM_VIDEO_SYNC_TIMESTAMP, null)

    override suspend fun getSecondaryFolderVideoSyncTime(): String? =
        context.getSharedPreferences(LAST_CAM_SYNC_TIMESTAMP_FILE,
            Context.MODE_PRIVATE).getString(KEY_SEC_VIDEO_SYNC_TIMESTAMP, null)


    /**
     * Keys for backing up time stamps
     */
    companion object {
        private const val KEY_CAM_SYNC_TIMESTAMP = "KEY_CAM_SYNC_TIMESTAMP"
        private const val KEY_CAM_VIDEO_SYNC_TIMESTAMP = "KEY_CAM_VIDEO_SYNC_TIMESTAMP"
        private const val KEY_SEC_SYNC_TIMESTAMP = "KEY_SEC_SYNC_TIMESTAMP"
        private const val KEY_SEC_VIDEO_SYNC_TIMESTAMP = "KEY_SEC_VIDEO_SYNC_TIMESTAMP"
        private const val KEY_PRIMARY_HANDLE = "KEY_PRIMARY_HANDLE"
        private const val KEY_SECONDARY_HANDLE = "KEY_SECONDARY_HANDLE"
        private const val LAST_CAM_SYNC_TIMESTAMP_FILE = "LAST_CAM_SYNC_TIMESTAMP_FILE"
    }
}