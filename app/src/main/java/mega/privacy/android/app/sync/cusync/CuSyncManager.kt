package mega.privacy.android.app.sync.cusync

import android.content.Intent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.SettingsConstants.ACTION_REENABLE_CAMERA_UPLOADS_PREFERENCE
import mega.privacy.android.app.constants.SettingsConstants.KEY_REENABLE_WHICH_PREFERENCE
import mega.privacy.android.app.jobservices.SyncRecord
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.sync.SyncListener
import mega.privacy.android.app.sync.cusync.callback.RemoveBackupCallback
import mega.privacy.android.app.sync.cusync.callback.SetBackupCallback
import mega.privacy.android.app.sync.cusync.callback.UpdateBackupCallback
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants.INVALID_NON_NULL_VALUE
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

object CuSyncManager {

    const val TYPE_BACKUP_PRIMARY = MegaApiJava.BACKUP_TYPE_CAMERA_UPLOAD
    const val TYPE_BACKUP_SECONDARY = MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS
    private const val PROGRESS_INVALID = -1
    private const val PROGRESS_FINISHED = 100
    private const val ACTIVE_HEARTBEAT_INTERVAL_SECONDS = 30L
    const val INACTIVE_HEARTBEAT_INTERVAL_SECONDS = 30 * 60

    private val megaApplication: MegaApplication = MegaApplication.getInstance()

    private val megaApi: MegaApiAndroid = megaApplication.megaApi

    private val databaseHandler: DatabaseHandler = DatabaseHandler.getDbHandler(megaApplication)

    private var activeHeartbeatTask: Disposable? = null

    private var cuPendingUploads = 0
    private var cuUploadedBytes = 0L
    private var cuLastActionTimestampSeconds = 0L
    private var cuLastUploadedHandle = INVALID_HANDLE

    private var muPendingUploads = 0
    private var muUploadedBytes = 0L
    private var muLastActionTimestampSeconds = 0L
    private var muLastUploadedHandle = INVALID_HANDLE

    fun setPrimaryBackup() =
        setBackup(
            TYPE_BACKUP_PRIMARY,
            databaseHandler.preferences?.camSyncHandle?.toLong(),
            databaseHandler.preferences?.camSyncLocalPath
        )


    fun setSecondaryBackup() =
        setBackup(
            TYPE_BACKUP_SECONDARY,
            databaseHandler.preferences?.megaHandleSecondaryFolder?.toLong(),
            databaseHandler.preferences?.localPathSecondaryFolder
        )


    private fun setBackup(
        backupType: Int,
        targetNode: Long?,
        localFolder: String?,
        state: Int = MegaApiJava.CU_SYNC_STATE_ACTIVE,
        subState: Int = MegaError.API_OK,
        extraData: String = INVALID_NON_NULL_VALUE
    ) {
        if (isInvalid(targetNode?.toString())) {
            logWarning("Target handle is invalid, value: $targetNode")
            reEnableCameraUploadsPreference(backupType)
            return
        }

        if (isInvalid(localFolder)) {
            logWarning("Local path is invalid, value: $localFolder")
            reEnableCameraUploadsPreference(backupType)
            return
        }

        // Same as localized CU/MU folder name.
        val backupName = if (backupType == TYPE_BACKUP_PRIMARY)
            StringResourcesUtils.getString(R.string.section_photo_sync)
        else
            StringResourcesUtils.getString(R.string.section_secondary_media_uploads)

        megaApi.setBackup(
            backupType,
            targetNode!!,
            localFolder,
            backupName,
            state,
            subState,
            extraData,
            SyncListener(SetBackupCallback(), megaApplication)
        )
    }

    fun updatePrimaryTargetNode(newTargetNode: Long) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        if (isInvalid(newTargetNode.toString())) {
            logWarning("Invalid target node, value: $newTargetNode")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            cuSync.apply {
                updateBackup(
                    backupId,
                    backupType,
                    newTargetNode,
                    localFolder,
                    backupName,
                    state,
                    subState,
                    extraData
                )
            }
        }
    }

    fun updateSecondaryTargetNode(newTargetNode: Long) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled.")
            return
        }

        if (isInvalid(newTargetNode.toString())) {
            logWarning("Invalid target node, value: $newTargetNode")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            muSync.apply {
                updateBackup(
                    backupId,
                    backupType,
                    newTargetNode,
                    localFolder,
                    backupName,
                    state,
                    subState,
                    extraData
                )
            }
        }
    }

    fun updatePrimaryLocalFolder(newLocalFolder: String?) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        if (isInvalid(newLocalFolder)) {
            logWarning("New local path is invalid, value: $newLocalFolder")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            cuSync.apply {
                updateBackup(
                    backupId,
                    backupType,
                    targetNode,
                    newLocalFolder!!,
                    backupName,
                    state,
                    subState,
                    extraData
                )
            }
        }
    }

    fun updateSecondaryLocalFolder(newLocalFolder: String?) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled, no need to update.")
            return
        }

        if (isInvalid(newLocalFolder)) {
            logWarning("New local path is invalid, value: $newLocalFolder")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            muSync.apply {
                updateBackup(
                    backupId,
                    backupType,
                    targetNode,
                    newLocalFolder!!,
                    backupName,
                    state,
                    subState,
                    extraData
                )
            }
        }
    }

    private fun updateBackup(
        backupId: Long,
        backupType: Int,
        targetNode: Long,
        localFolder: String,
        deviceId: String,
        state: Int,
        subState: Int,
        extraData: String
    ) {
        if (isInvalid(backupId.toString())) {
            logWarning("Invalid sync id, value: $backupId")
            return
        }

        megaApi.updateBackup(
            backupId,
            backupType,
            targetNode,
            localFolder,
            deviceId,
            state,
            subState,
            extraData,
            SyncListener(UpdateBackupCallback(), megaApplication)
        )
    }

    fun removePrimaryBackup() {
        removeBackup(databaseHandler.cuBackup?.backupId)
    }

    fun removeSecondaryBackup() {
        removeBackup(databaseHandler.muBackup?.backupId)
    }

    private fun removeBackup(id: Long?) {
        id?.let {
            megaApi.removeBackup(id, SyncListener(RemoveBackupCallback(), megaApplication))
        }
    }

    private fun isInvalid(value: String?) =
        TextUtil.isTextEmpty(value) || INVALID_NON_NULL_VALUE == value

    fun startActiveHeartbeat(records: List<SyncRecord>) {
        if (records.isEmpty()) {
            return
        }

        cuPendingUploads = 0
        var cuTotalUploadBytes = 0L
        muPendingUploads = 0
        var muTotalUploadBytes = 0L

        for (record in records) {
            if (record.isCopyOnly) {
                continue
            }

            val bytes = File(record.localPath).length()
            if (record.isSecondary) {
                muPendingUploads++
                muTotalUploadBytes += bytes
            } else {
                cuPendingUploads++
                cuTotalUploadBytes += bytes
            }
        }

        if (CameraUploadUtil.isPrimaryEnabled()) {
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
        }
        if (CameraUploadUtil.isSecondaryEnabled()) {
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
        }

        activeHeartbeatTask = Observable.interval(0L, ACTIVE_HEARTBEAT_INTERVAL_SECONDS, SECONDS)
            .subscribe({
                val cuBackup = databaseHandler.cuBackup
                if (CameraUploadUtil.isPrimaryEnabled() && cuBackup != null && cuTotalUploadBytes != 0L) {
                    megaApi.sendBackupHeartbeat(
                        cuBackup.backupId,
                        MegaApiJava.CU_SYNC_STATUS_SYNCING,
                        (cuUploadedBytes / cuTotalUploadBytes.toFloat() * 100).toInt(),
                        cuPendingUploads,
                        0,
                        cuLastActionTimestampSeconds,
                        cuLastUploadedHandle,
                        null
                    )
                }

                val muBackup = databaseHandler.muBackup
                if (CameraUploadUtil.isSecondaryEnabled() && muBackup != null && muTotalUploadBytes != 0L) {
                    megaApi.sendBackupHeartbeat(
                        muBackup.backupId,
                        MegaApiJava.CU_SYNC_STATUS_SYNCING,
                        (muUploadedBytes / muTotalUploadBytes.toFloat() * 100).toInt(),
                        muPendingUploads,
                        0,
                        muLastActionTimestampSeconds,
                        muLastUploadedHandle,
                        null
                    )
                }
            }, logErr("CuSyncManager startActiveHeartbeat"))
    }

    fun onUploadSuccess(node: MegaNode, record: SyncRecord) {
        val bytes = File(record.localPath).length()
        if (record.isSecondary) {
            muPendingUploads--
            muUploadedBytes += bytes
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            muLastUploadedHandle = node.handle
        } else {
            cuPendingUploads--
            cuUploadedBytes += bytes
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            cuLastUploadedHandle = node.handle
        }
    }

    fun reportUploadFinish() {
        val cuBackup = databaseHandler.cuBackup
        if (cuBackup != null && cuLastUploadedHandle != INVALID_HANDLE) {
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            megaApi.sendBackupHeartbeat(
                cuBackup.backupId, MegaApiJava.CU_SYNC_STATUS_SYNCING, PROGRESS_FINISHED, 0, 0,
                cuLastActionTimestampSeconds, cuLastUploadedHandle, null
            )
        }

        val muBackup = databaseHandler.muBackup
        if (muBackup != null && muLastUploadedHandle != INVALID_HANDLE) {
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            megaApi.sendBackupHeartbeat(
                muBackup.backupId, MegaApiJava.CU_SYNC_STATUS_SYNCING, PROGRESS_FINISHED, 0, 0,
                muLastActionTimestampSeconds, muLastUploadedHandle, null
            )
        }
    }

    fun stopActiveHeartbeat() {
        activeHeartbeatTask?.dispose()
        activeHeartbeatTask = null
        cuPendingUploads = 0
        cuUploadedBytes = 0
        muPendingUploads = 0
        muUploadedBytes = 0
    }

    fun doInactiveHeartbeat(onFinish: () -> Unit) {
        val cuBackup = databaseHandler.cuBackup
        if (cuBackup != null && CameraUploadUtil.isPrimaryEnabled()) {
            megaApi.sendBackupHeartbeat(
                cuBackup.backupId, MegaApiJava.CU_SYNC_STATUS_UPTODATE, PROGRESS_INVALID, 0, 0,
                0, cuLastUploadedHandle, createOnFinishListener(onFinish)
            )
        }

        val muBackup = databaseHandler.muBackup
        if (muBackup != null && CameraUploadUtil.isSecondaryEnabled()) {
            megaApi.sendBackupHeartbeat(
                muBackup.backupId, MegaApiJava.CU_SYNC_STATUS_UPTODATE, PROGRESS_INVALID, 0, 0,
                0, muLastUploadedHandle, createOnFinishListener(onFinish)
            )
        }
    }

    private fun createOnFinishListener(onFinish: () -> Unit): MegaRequestListenerInterface {
        return object : BaseListener(null) {
            override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
                onFinish()
            }
        }
    }

    fun isActive() = activeHeartbeatTask != null

    fun reEnableCameraUploadsPreference(which: Int) = MegaApplication.getInstance()
        .sendBroadcast(Intent(ACTION_REENABLE_CAMERA_UPLOADS_PREFERENCE).putExtra(KEY_REENABLE_WHICH_PREFERENCE, which))
}
