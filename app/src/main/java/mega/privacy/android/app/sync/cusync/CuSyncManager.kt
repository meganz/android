package mega.privacy.android.app.sync.cusync

import android.content.Intent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_REENABLE_CU_PREFERENCE
import mega.privacy.android.app.constants.BroadcastConstants.KEY_REENABLE_WHICH_PREFERENCE
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.jobservices.SyncRecord
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.sync.SyncListener
import mega.privacy.android.app.sync.cusync.callback.RemoveBackupCallback
import mega.privacy.android.app.sync.cusync.callback.SetBackupCallback
import mega.privacy.android.app.sync.cusync.callback.UpdateBackupCallback
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Manager class used to launch CU(Camera Uploads) backup related requests and send sync heartbeat.
 * CU refers to Camera Uploads, primary.
 * MU refers to Media Uploads, secondary.
 */
object CuSyncManager {

    private const val PROGRESS_FINISHED = 100

    /**
     * Backup state,
     * originally defined in heartbeats.h
        enum State {
            ACTIVE = 1,             // Working fine (enabled)
            FAILED = 2,             // Failed (permanently disabled)
            TEMPORARY_DISABLED = 3, // Temporarily disabled due to a transient situation (e.g: account blocked). Will be resumed when the condition passes
            DISABLED = 4,           // Disabled by the user
            PAUSE_UP = 5,           // Active but upload transfers paused in the SDK
            PAUSE_DOWN = 6,         // Active but download transfers paused in the SDK
            PAUSE_FULL = 7,         // Active but transfers paused in the SDK
        };
     */
    object State {
        const val CU_SYNC_STATE_ACTIVE = 1
        const val CU_SYNC_STATE_FAILED = 2
        const val CU_SYNC_STATE_TEMPORARY_DISABLED = 3
        const val CU_SYNC_STATE_DISABLED = 4
        const val CU_SYNC_STATE_PAUSE_UP = 5
        const val CU_SYNC_STATE_PAUSE_DOWN = 6
        const val CU_SYNC_STATE_PAUSE_FULL = 7
    }

    /**
     * Heartbeat status,
     * originally defined in heartbeats.h
        enum Status {
            UPTODATE = 1, // Up to date: local and remote paths are in sync
            SYNCING = 2, // The sync engine is working, transfers are in progress
            PENDING = 3, // The sync engine is working, e.g: scanning local folders
            INACTIVE = 4, // Sync is not active. A state != ACTIVE should have been sent through '''sp'''
            UNKNOWN = 5, // Unknown status
        };
     */
    object Status {
        const val CU_SYNC_STATUS_UPTODATE = 1
        const val CU_SYNC_STATUS_SYNCING = 2
        const val CU_SYNC_STATUS_PENDING = 3
        const val CU_SYNC_STATUS_INACTIVE = 4
        const val CU_SYNC_STATUS_UNKNOWN = 5
    }

    /**
     * While CU process is running, send heartbeat every 30s.
     */
    private const val ACTIVE_HEARTBEAT_INTERVAL_SECONDS = 30L

    /**
     * When the app is inactive, send heartbeat every 30m.
     */
    const val INACTIVE_HEARTBEAT_INTERVAL_SECONDS = 30 * 60

    private val megaApplication = MegaApplication.getInstance()

    private val megaApi = megaApplication.megaApi

    private val databaseHandler: DatabaseHandler = megaApplication.dbH

    /**
     * Periodically execute task, used to send active sync heartbeat.
     */
    private var activeHeartbeatTask: Disposable? = null

    /**
     * How many files will be uploaded as CU backup.
     */
    private var cuPendingUploads = 0

    /**
     * Total size of all the files will be uploaded as CU backup.
     */
    private var cuUploadedBytes = 0L

    /**
     * When was the last file uploaded as CU backup, time unit, second.
     */
    private var cuLastActionTimestampSeconds = 0L

    /**
     * Handle of last uploaded file as CU backup.
     */
    private var cuLastUploadedHandle = INVALID_HANDLE

    /**
     * How many files will be uploaded as MU backup.
     */
    private var muPendingUploads = 0

    /**
     * Total size of all the files will be uploaded as MU backup.
     */
    private var muUploadedBytes = 0L

    /**
     * When was the last file uploaded as MU backup, time unit, second.
     */
    private var muLastActionTimestampSeconds = 0L

    /**
     * Handle of last uploaded file as MU backup.
     */
    private var muLastUploadedHandle = INVALID_HANDLE

    /**
     * Create CU(Camera Uploads) backup. Should be called only when CU is enabled.
     */
    fun setPrimaryBackup() =
        setBackup(
            BACKUP_TYPE_CAMERA_UPLOADS,
            databaseHandler.preferences?.camSyncHandle?.toLong(),
            databaseHandler.preferences?.camSyncLocalPath
        )

    /**
     * Create CU(Media Uploads) backup. Should be called only when MU is enabled.
     */
    fun setSecondaryBackup() =
        setBackup(
            BACKUP_TYPE_MEDIA_UPLOADS,
            databaseHandler.preferences?.megaHandleSecondaryFolder?.toLong(),
            databaseHandler.preferences?.localPathSecondaryFolder
        )

    /**
     * Create backup with given info.
     *
     * @param backupType Type of the backup,
     * should be MegaApiJava.BACKUP_TYPE_CAMERA_UPLOAD or MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS
     * @param targetNode Handle of the MegaNode where the backup targets to.
     * @param localFolder Path of the local folder where the backup uploads from.
     * @param state state Should be MegaApiJava.CU_SYNC_STATE_ACTIVE when create.
     * @param subState subState Valid value definitions
     * @see MegaError
     * @param extraData extraData
     */
    private fun setBackup(
        backupType: Int,
        targetNode: Long?,
        localFolder: String?,
        state: Int = State.CU_SYNC_STATE_ACTIVE,
        subState: Int = MegaError.API_OK,
        extraData: String = INVALID_NON_NULL_VALUE
    ) {
        if (isInvalid(targetNode?.toString())) {
            logWarning("Target handle is invalid, value: $targetNode")
            reEnableCameraUploadsPreference(backupType)
            return
        }

        if (isInvalid(localFolder)) {
            logWarning("Local path is invalid.")
            reEnableCameraUploadsPreference(backupType)
            return
        }

        // Same as localized CU/MU folder name.
        val backupName = if (backupType == BACKUP_TYPE_CAMERA_UPLOADS)
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

    /**
     * Update CU backup's target node when target node changes.
     * There're two cases:
     * 1. The change is made on the same device(trigger set CU attribute)
     * 2. The change is made on another device login with the same account.(trigger get CU attribute)
     *
     * This method will be called when get/set CU attribute successfully, and update local database.
     * @see DatabaseHandler.setCamSyncHandle
     */
    fun updatePrimaryTargetNode(newTargetNode: Long) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        if (isInvalid(newTargetNode.toString())) {
            logWarning("Invalid target node, value: $newTargetNode")
            return
        }

        // If hasn't created one, set one instead.
        val cuSync = databaseHandler.cuBackup
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            updateBackup(
                cuSync.backupId,
                newTargetNode,
                null,
                INVALID_VALUE
            )
        }
    }

    /**
     * Update MU backup's target node when target node changes.
     * There're two cases:
     * 1. The change is made on the same device(trigger set MU attribute)
     * 2. The change is made on another device login with the same account.(trigger get MU attribute)
     *
     * This method will be called when get/set MU attribute successfully, and update local database.
     * @see DatabaseHandler.setSecondaryFolderHandle
     */
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
        // If hasn't created one, set one instead.
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            updateBackup(
                muSync.backupId,
                newTargetNode,
                null,
                INVALID_VALUE,
            )
        }
    }

    /**
     * Update CU backup's local folder when set to another local folder.
     * This method will be called when a local folder is selected.
     *
     * @see SettingsConstants.REQUEST_CAMERA_FOLDER
     */
    fun updatePrimaryLocalFolder(newLocalFolder: String?) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        if (isInvalid(newLocalFolder)) {
            logWarning("New local path is invalid.")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            updateBackup(
                cuSync.backupId,
                INVALID_HANDLE,
                newLocalFolder!!,
                INVALID_VALUE
            )
        }
    }

    /**
     * Update MU backup's local folder when set to another local folder.
     * This method will be called when a local folder is selected.
     *
     * @see SettingsConstants.REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER
     */
    fun updateSecondaryLocalFolder(newLocalFolder: String?) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled, no need to update.")
            return
        }

        if (isInvalid(newLocalFolder)) {
            logWarning("New local path is invalid.")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            updateBackup(
                muSync.backupId,
                INVALID_HANDLE,
                newLocalFolder!!,
                INVALID_VALUE
            )
        }
    }

    /**
     * Update CU backup's state.
     *
     * @see MegaApiJava
     */
    fun updatePrimaryBackupState(newState: Int) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled, no need to update.")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync != null && newState != cuSync.state) {
            updateBackup(
                cuSync.backupId,
                INVALID_HANDLE,
                null,
                newState
            )
        }
    }

    /**
     * Update CU backup's state.
     *
     * @see MegaApiJava
     */
    fun updateSecondaryBackupState(newState: Int) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled, no need to update.")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync != null && newState != muSync.state) {
            updateBackup(
                muSync.backupId,
                INVALID_HANDLE,
                null,
                newState
            )
        }
    }

    /**
     * Update a backup.
     * For the values keep the same, pass invliad value to avoid to send to the server.
     * @see MegaApiJava.updateBackup
     *
     * @param backupId ID of the backup which is to be updated.
     * should be MegaApiJava.BACKUP_TYPE_CAMERA_UPLOAD or MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS
     * @param targetNode Handle of the MegaNode where the backup targets to.
     * @param localFolder Path of the local folder where the backup uploads from.
     * @param state state Current state of the backup.
     */
    private fun updateBackup(
        backupId: Long,
        targetNode: Long,
        localFolder: String?,
        state: Int
    ) {
        if (isInvalid(backupId.toString())) {
            logWarning("Invalid sync id, value: $backupId")
            return
        }

        megaApi.updateBackup(
            backupId,
            BACKUP_TYPE_INVALID,
            targetNode,
            localFolder,
            state,
            MegaError.API_OK,
            INVALID_NON_NULL_VALUE,
            SyncListener(UpdateBackupCallback(), megaApplication)
        )
    }

    /**
     * Remove CU backup.
     */
    fun removePrimaryBackup() {
        removeBackup(databaseHandler.cuBackup?.backupId)
    }

    /**
     * Remove MU backup.
     */
    fun removeSecondaryBackup() {
        removeBackup(databaseHandler.muBackup?.backupId)
    }

    /**
     * Remove backup by its ID.
     *
     * @param id ID of the backup to be removed.
     */
    private fun removeBackup(id: Long?) {
        id?.let {
            megaApi.removeBackup(id, SyncListener(RemoveBackupCallback(), megaApplication))
        }
    }

    /**
     * Check if a string is invalid value, including null check and value check(whether a non-null invalid value).
     *
     * @param value String value to be checked.
     * @return true, if the string is null, empty or invalid value. false, otherwise.
     */
    private fun isInvalid(value: String?) =
        TextUtil.isTextEmpty(value) || INVALID_NON_NULL_VALUE == value

    /**
     * Start to send sync heartbeats when CU process starts.
     *
     * @param records Pending upload/copy files.
     */
    fun startActiveHeartbeat(records: List<SyncRecord>) {
        if (records.isEmpty()) {
            return
        }

        cuPendingUploads = 0
        var cuTotalUploadBytes = 0L
        muPendingUploads = 0
        var muTotalUploadBytes = 0L

        for (record in records) {
            val bytes = File(record.localPath).length()
            if (record.isSecondary) {
                muPendingUploads++
                muTotalUploadBytes += bytes
            } else {
                cuPendingUploads++
                cuTotalUploadBytes += bytes
            }
        }
        logDebug("CU pending upload file $cuPendingUploads, size: $cuTotalUploadBytes; MU pending upload file $muPendingUploads, size: $muTotalUploadBytes")

        if (CameraUploadUtil.isPrimaryEnabled()) {
            updatePrimaryBackupState(State.CU_SYNC_STATE_ACTIVE)
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
        }

        if (CameraUploadUtil.isSecondaryEnabled()) {
            updateSecondaryBackupState(State.CU_SYNC_STATE_ACTIVE)
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
        }

        activeHeartbeatTask = Observable.interval(0L, ACTIVE_HEARTBEAT_INTERVAL_SECONDS, SECONDS)
            .subscribe({
                val cuBackup = databaseHandler.cuBackup
                if (CameraUploadUtil.isPrimaryEnabled() && cuBackup != null && cuTotalUploadBytes != 0L && cuBackup.state != State.CU_SYNC_STATE_PAUSE_UP) {
                    logDebug("Send CU heartbeat.")
                    megaApi.sendBackupHeartbeat(
                        cuBackup.backupId,
                        Status.CU_SYNC_STATUS_SYNCING,
                        (cuUploadedBytes / cuTotalUploadBytes.toFloat() * 100).toInt(),
                        cuPendingUploads,
                        0,
                        cuLastActionTimestampSeconds,
                        cuLastUploadedHandle,
                        null
                    )
                }

                val muBackup = databaseHandler.muBackup
                if (CameraUploadUtil.isSecondaryEnabled() && muBackup != null && muTotalUploadBytes != 0L && muBackup.state != State.CU_SYNC_STATE_PAUSE_UP) {
                    logDebug("Send MU heartbeat.")
                    megaApi.sendBackupHeartbeat(
                        muBackup.backupId,
                        Status.CU_SYNC_STATUS_SYNCING,
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

    /**
     * Callback when a file is uploaded/a node is copied to the target node.
     *
     * @param node The uploaded/copied node.
     * @param isSecondary Whether it is a MU backup.
     */
    fun onUploadSuccess(node: MegaNode, isSecondary: Boolean) {
        if (isSecondary) {
            muPendingUploads--
            muUploadedBytes += node.size
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            muLastUploadedHandle = node.handle
        } else {
            cuPendingUploads--
            cuUploadedBytes += node.size
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            cuLastUploadedHandle = node.handle
        }
    }

    /**
     * Callback when the process finished, report to server.
     */
    fun reportUploadFinish() {
        val cuBackup = databaseHandler.cuBackup
        if (cuBackup != null && cuLastUploadedHandle != INVALID_HANDLE) {
            logDebug("CU sync finished at $cuLastActionTimestampSeconds, last uploaded handle is $cuLastUploadedHandle backup id:${cuBackup.backupId}")
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            megaApi.sendBackupHeartbeat(
                cuBackup.backupId,
                Status.CU_SYNC_STATUS_UPTODATE,
                PROGRESS_FINISHED,
                0,
                0,
                cuLastActionTimestampSeconds,
                cuLastUploadedHandle,
                null
            )
        }

        val muBackup = databaseHandler.muBackup
        if (muBackup != null && muLastUploadedHandle != INVALID_HANDLE) {
            logDebug("MU sync finished at $muLastActionTimestampSeconds, last uploaded handle is $muLastUploadedHandle backup id:${muBackup.backupId}")
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            megaApi.sendBackupHeartbeat(
                muBackup.backupId,
                Status.CU_SYNC_STATUS_UPTODATE,
                PROGRESS_FINISHED,
                0,
                0,
                muLastActionTimestampSeconds,
                muLastUploadedHandle,
                null
            )
        }
    }

    /**
     * Callback when the process is interrupted, report to server.
     */
    fun reportUploadInterrupted() {
        val cuBackup = databaseHandler.cuBackup
        if (cuBackup != null && cuLastUploadedHandle != INVALID_HANDLE) {
            logDebug("CU sync is interrupted.")
            cuLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            megaApi.sendBackupHeartbeat(
                cuBackup.backupId,
                Status.CU_SYNC_STATUS_INACTIVE,
                INVALID_VALUE,
                cuPendingUploads,
                0,
                cuLastActionTimestampSeconds,
                cuLastUploadedHandle,
                null
            )
        }

        val muBackup = databaseHandler.muBackup
        if (muBackup != null && muLastUploadedHandle != INVALID_HANDLE) {
            logDebug("MU sync is interrupted.")
            muLastActionTimestampSeconds = System.currentTimeMillis() / 1000
            megaApi.sendBackupHeartbeat(
                muBackup.backupId,
                Status.CU_SYNC_STATUS_INACTIVE,
                INVALID_VALUE,
                muPendingUploads,
                0,
                muLastActionTimestampSeconds,
                muLastUploadedHandle,
                null
            )
        }
    }

    /**
     * Stop send active heartbeat and reset when CameraUploadsService destroys.
     */
    fun stopActiveHeartbeat() {
        activeHeartbeatTask?.dispose()
        activeHeartbeatTask = null
        cuPendingUploads = 0
        cuUploadedBytes = 0
        muPendingUploads = 0
        muUploadedBytes = 0
    }

    /**
     * When the app is inactive, send heartbeat as well.
     */
    fun doInactiveHeartbeat(onFinish: () -> Unit) {
        val cuBackup = databaseHandler.cuBackup
        if (cuBackup != null && CameraUploadUtil.isPrimaryEnabled()) {
            megaApi.sendBackupHeartbeat(
                cuBackup.backupId,
                Status.CU_SYNC_STATUS_INACTIVE,
                INVALID_VALUE,
                0,
                0,
                0,
                cuLastUploadedHandle,
                createOnFinishListener(onFinish)
            )
        }

        val muBackup = databaseHandler.muBackup
        if (muBackup != null && CameraUploadUtil.isSecondaryEnabled()) {
            megaApi.sendBackupHeartbeat(
                muBackup.backupId,
                Status.CU_SYNC_STATUS_INACTIVE,
                INVALID_VALUE,
                0,
                0,
                0,
                muLastUploadedHandle,
                createOnFinishListener(onFinish)
            )
        }
    }

    /**
     * Create a listener to listen to the send inactive heartbeat event.
     *
     * @param onFinish Callback when the request finished.
     *
     * @return MegaRequestListenerInterface object listen to the request.
     */
    private fun createOnFinishListener(onFinish: () -> Unit): MegaRequestListenerInterface {
        return object : BaseListener(null) {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                onFinish()
            }
        }
    }

    /**
     * Check if CU process is running by checking if there's a non-null active send heartbeat task.
     *
     * @return true, CU process is running, otherwise, false.
     */
    fun isActive() = activeHeartbeatTask != null

    /**
     * Send a broadcast to re-enable CU/MU preference in settings fragment.
     * In order to prevent user enable/disable CU/MU too fast,
     * after enabled CU/MU, the corresponding preference in settings fragment will be set as disabled.
     * After setBackup finished, need to re-enable the preference.
     *
     * @param which Re-enable which preference, CU or MU.
     */
    fun reEnableCameraUploadsPreference(which: Int) = MegaApplication.getInstance()
        .sendBroadcast(
            Intent(BROADCAST_ACTION_REENABLE_CU_PREFERENCE).putExtra(
                KEY_REENABLE_WHICH_PREFERENCE,
                which
            )
        )
}
