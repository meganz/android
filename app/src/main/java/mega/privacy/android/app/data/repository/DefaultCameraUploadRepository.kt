package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.SyncRecord
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.app.utils.FileUtil
import timber.log.Timber
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Inject

/**
 * Default implementation of [CameraUploadRepository]
 *
 * @property databaseHandler DatabaseHandler
 * @property context Context
 * @property megaApiGateway MegaApiGateway
 * @property threadPoolExecutor ThreadPoolExecutor
 * @property ioDispatcher CoroutineDispatcher
 */
class DefaultCameraUploadRepository @Inject constructor(
    private val databaseHandler: DatabaseHandler,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val threadPoolExecutor: ThreadPoolExecutor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CameraUploadRepository {

    init {
        initPreferencesIfNull()
    }

    private fun initPreferencesIfNull() {
        if (databaseHandler.preferences == null) {
            Timber.w("databaseHandler.preferences is NULL")
            databaseHandler.setStorageAskAlways(true)
            val defaultDownloadLocation = FileUtil.buildDefaultDownloadDir(context)
            defaultDownloadLocation.mkdirs()
            databaseHandler.setStorageDownloadLocation(defaultDownloadLocation.absolutePath)
        }
    }

    /**
     * All different sync timestamps
     */
    enum class SyncTimeStamp {
        /**
         * only primary photos
         */
        PRIMARY,

        /**
         * primary videos
         */
        PRIMARY_VIDEO,

        /**
         * only secondary photos
         */
        SECONDARY,

        /**
         * secondary videos
         */
        SECONDARY_VIDEO
    }

    override fun isSyncByWifi() =
        databaseHandler.preferences.camSyncWifi.toBoolean()

    override fun getSyncTimeStamp() =
        databaseHandler.preferences.camSyncTimeStamp?.toLongOrNull() ?: 0

    override fun getVideoSyncTimeStamp() =
        databaseHandler.preferences.camVideoSyncTimeStamp?.toLongOrNull() ?: 0

    override fun getSecondarySyncTimeStamp(): String? =
        databaseHandler.preferences.secSyncTimeStamp

    override fun getSecondaryVideoSyncTimeStamp(): String? =
        databaseHandler.preferences.secVideoSyncTimeStamp

    override fun getPendingSyncRecords(): List<SyncRecord> =
        databaseHandler.findAllPendingSyncRecords()

    override fun manageSyncFileUpload(
        handlePreference: (preference: Int) -> Unit,
        noPreference: () -> Unit,
    ) {
        val fileUpload = databaseHandler.preferences.camSyncFileUpload
        if (fileUpload != null && fileUpload.toIntOrNull() != null) {
            handlePreference(fileUpload.toInt())
        } else {
            databaseHandler.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS)
            noPreference()
        }
    }

    override fun deleteSyncRecord(path: String?, isSecondary: Boolean) =
        databaseHandler.deleteSyncRecordByPath(path, isSecondary)

    override fun deleteSyncRecordLocalPath(localPath: String?, isSecondary: Boolean) =
        databaseHandler.deleteSyncRecordByLocalPath(localPath, isSecondary)

    override fun getSyncRecordOrNull(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? = databaseHandler.recordExists(fingerprint, isSecondary, isCopy)

    override fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = databaseHandler.fileNameExists(fileName, isSecondary, type)

    override fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean = databaseHandler.localPathExists(fileName, isSecondary, type)

    override fun saveSyncRecord(record: SyncRecord) = databaseHandler.saveSyncRecord(record)

    override fun setSyncTimeStamp(timestamp: Long, type: SyncTimeStamp) {
        when (type) {
            SyncTimeStamp.PRIMARY -> databaseHandler.setCamSyncTimeStamp(timestamp)
            SyncTimeStamp.PRIMARY_VIDEO -> databaseHandler.setCamVideoSyncTimeStamp(timestamp)
            SyncTimeStamp.SECONDARY -> databaseHandler.setSecSyncTimeStamp(timestamp)
            SyncTimeStamp.SECONDARY_VIDEO -> databaseHandler.setSecVideoSyncTimeStamp(timestamp)
        }
    }
}
