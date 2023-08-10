package mega.privacy.android.app.sync.camerauploads

import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_REENABLE_CU_PREFERENCE
import mega.privacy.android.app.constants.BroadcastConstants.KEY_REENABLE_WHICH_PREFERENCE
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants.INVALID_NON_NULL_VALUE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.user.UserCredentials
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS
import nz.mega.sdk.MegaApiJava.BACKUP_TYPE_INVALID
import nz.mega.sdk.MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber

/**
 * Manager class used to launch CU(Camera Uploads) backup related requests and send sync heartbeat.
 * CU refers to Camera Uploads, primary.
 * MU refers to Media Uploads, secondary.
 */
object CameraUploadSyncManager {

    private const val PROGRESS_FINISHED = 100

    private val megaApplication = MegaApplication.getInstance()

    private val megaApi = megaApplication.megaApi

    private val databaseHandler = megaApplication.dbH

    /**
     * Handle of last uploaded file as CU backup.
     */
    private var cuLastUploadedHandle = INVALID_HANDLE

    /**
     * Handle of last uploaded file as MU backup.
     */
    private var muLastUploadedHandle = INVALID_HANDLE

    /**
     * Create CU (Camera Uploads) backup. Should be called only when CU is enabled.
     */
    fun setPrimaryBackup() = setBackup(
        backupType = BACKUP_TYPE_CAMERA_UPLOADS,
        targetNode = databaseHandler.preferences?.camSyncHandle?.toLong(),
        localFolder = databaseHandler.preferences?.camSyncLocalPath
    )

    /**
     * Create MU (Media Uploads) backup. Should be called only when MU is enabled.
     */
    fun setSecondaryBackup() = setBackup(
        backupType = BACKUP_TYPE_MEDIA_UPLOADS,
        targetNode = databaseHandler.preferences?.megaHandleSecondaryFolder?.toLong(),
        localFolder = databaseHandler.preferences?.localPathSecondaryFolder
    )

    /**
     * Create backup with given info.
     *
     * The Backup State is set as [BackupState.ACTIVE] and the
     * Sub-State as [MegaError.API_OK]
     *
     * @param backupType Either [MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS] or [MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS]
     * @param targetNode The [MegaNode] handle where the Backup targets to.
     * @param localFolder The local folder path where the Backup uploads from.
     * @see MegaError
     */
    private fun setBackup(
        backupType: Int,
        targetNode: Long?,
        localFolder: String?,
    ) {
        if (targetNode == null || isInvalid(targetNode.toString())) {
            Timber.w("Target handle is invalid, value: $targetNode")
            reEnableCameraUploadsPreference(backupType)
            return
        }

        if (isInvalid(localFolder)) {
            Timber.w("Local path is invalid.")
            reEnableCameraUploadsPreference(backupType)
            return
        }

        // Same as localized CU/MU folder name.
        val backupName = if (backupType == BACKUP_TYPE_CAMERA_UPLOADS)
            megaApplication.getString(R.string.section_photo_sync)
        else
            megaApplication.getString(R.string.section_secondary_media_uploads)

        megaApi.setBackup(
            backupType,
            targetNode,
            localFolder,
            backupName,
            BackupState.ACTIVE.value,
            MegaError.API_OK,
            onBackupSet(backupType),
        )
    }

    /**
     * The listener for when the Backup is set
     *
     * @param backupType The Backup type
     */
    private fun onBackupSet(backupType: Int) =
        OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
            if (error.errorCode == MegaError.API_OK) {
                Timber.d("Request ${request.type}: ${request.requestString} successfully")
                with(request) {
                    val backup = Backup(
                        backupId = parentHandle,
                        backupType = totalBytes.toInt(),
                        targetNode = nodeHandle,
                        localFolder = file,
                        backupName = name,
                        state = BackupState.fromValue(access),
                        subState = numDetails,
                        extraData = INVALID_NON_NULL_VALUE,
                        targetFolderPath = INVALID_NON_NULL_VALUE

                    )
                    Timber.d("Save Backup $backup to local cache.")
                    databaseHandler.saveBackup(backup)
                    reEnableCameraUploadsPreference(totalBytes.toInt())

                    // After setting up the Backup folder, immediately send an Unknown Heartbeat
                    when (backupType) {
                        BACKUP_TYPE_CAMERA_UPLOADS -> sendPrimaryFolderHeartbeat(HeartbeatStatus.UNKNOWN)
                        BACKUP_TYPE_MEDIA_UPLOADS -> sendSecondaryFolderHeartbeat(HeartbeatStatus.UNKNOWN)
                        else -> Unit
                    }
                }
            } else {
                Timber.w("Request ${request.type}: ${request.requestString} failed, ${error.errorString}: ${error.errorCode}")
                // Re-enable preference in settings fragment.
                reEnableCameraUploadsPreference(request.totalBytes.toInt())
            }
        })

    /**
     * Sends a Heartbeat for the Primary Folder
     *
     * @param heartbeatStatus The heartbeat status
     */
    @Deprecated(
        message = "Replace all usages with use case",
        replaceWith = ReplaceWith("SendCameraUploadsBackupHeartBeatUseCase")
    )
    fun sendPrimaryFolderHeartbeat(heartbeatStatus: HeartbeatStatus) {
        val cuBackup = databaseHandler.cuBackup
        // When the Heartbeat Status is UP_TO_DATE, immediately set its value to PROGRESS_FINISHED (100)
        // Otherwise, default to 0
        val syncProgressValue = when (heartbeatStatus) {
            HeartbeatStatus.UP_TO_DATE -> PROGRESS_FINISHED
            else -> 0
        }

        if (cuBackup != null && CameraUploadUtil.isPrimaryEnabled()) {
            Timber.d("Sending Primary Folder Heartbeat, backupId = ${cuBackup.backupId}, Heartbeat Status = ${heartbeatStatus.name}")
            megaApi.sendBackupHeartbeat(
                cuBackup.backupId,
                heartbeatStatus.value,
                syncProgressValue,
                0,
                0,
                0,
                cuLastUploadedHandle,
                createHeartbeatListener()
            )
        }
    }

    /**
     * Sends a Heartbeat for the Secondary Folder
     *
     * @param heartbeatStatus The heartbeat status
     */
    @Deprecated(
        message = "Replace all usages with use case",
        replaceWith = ReplaceWith("SendMediaUploadsBackupHeartBeatUseCase")
    )
    fun sendSecondaryFolderHeartbeat(heartbeatStatus: HeartbeatStatus) {
        val muBackup = databaseHandler.muBackup
        // When the Heartbeat Status is UP_TO_DATE, immediately set its value to PROGRESS_FINISHED (100)
        // Otherwise, default to 0
        val syncProgressValue = when (heartbeatStatus) {
            HeartbeatStatus.UP_TO_DATE -> PROGRESS_FINISHED
            else -> 0
        }

        if (muBackup != null && CameraUploadUtil.isSecondaryEnabled()) {
            Timber.d("Sending Secondary Folder Heartbeat, backupId = ${muBackup.backupId}, Heartbeat Status = ${heartbeatStatus.name}")
            megaApi.sendBackupHeartbeat(
                muBackup.backupId,
                heartbeatStatus.value,
                syncProgressValue,
                0,
                0,
                0,
                muLastUploadedHandle,
                createHeartbeatListener()
            )
        }
    }

    /**
     * Update the Primary Folder target node when it changes
     *
     * There are two cases:
     * 1. The change is made on the same device(trigger set CU attribute)
     * 2. The change is made on another device login with the same account (trigger get CU attribute)
     *
     * This method will be called when get/set CU attribute is called successfully, and the
     * local database has been updated
     *
     * @see DatabaseHandler.setCamSyncHandle
     *
     * @param newTargetNode The new target node
     */
    fun updatePrimaryFolderTargetNode(newTargetNode: Long) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            Timber.d("Primary Folder is disabled. Unable to update Primary Folder node")
            return
        }

        if (isInvalid(newTargetNode.toString())) {
            Timber.w("Invalid target node, value: $newTargetNode")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            updateBackup(
                backupId = cuSync.backupId,
                targetNode = newTargetNode,
                localFolder = null,
                backupName = megaApplication.getString(R.string.section_photo_sync),
                backupState = BackupState.INVALID,
            )
        }
    }

    /**
     * Update the Secondary Folder target node when it changes
     *
     * There are two cases:
     * 1. The change is made on the same device (trigger set MU attribute)
     * 2. The change is made on another device login with the same account (trigger get MU attribute)
     *
     * This method will be called when get/set MU attribute is called successfully, and the
     * local database has been updated.
     *
     * @see DatabaseHandler.setSecondaryFolderHandle
     *
     * @param newTargetNode The new target node
     */
    fun updateSecondaryFolderTargetNode(newTargetNode: Long) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            Timber.d("Secondary Folder is disabled. Unable to update Secondary Folder node")
            return
        }

        if (isInvalid(newTargetNode.toString())) {
            Timber.w("Invalid target node, value: $newTargetNode")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            updateBackup(
                backupId = muSync.backupId,
                targetNode = newTargetNode,
                localFolder = null,
                backupName = megaApplication.getString(R.string.section_secondary_media_uploads),
                backupState = BackupState.INVALID,
            )
        }
    }

    /**
     * Update the Primary local folder, when set to another local folder
     * This method will be called when a local folder is selected.
     *
     * @see SettingsConstants.REQUEST_CAMERA_FOLDER
     *
     * @param newLocalFolder The path of the new Primary local folder
     */
    fun updatePrimaryLocalFolder(newLocalFolder: String?) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            Timber.d("Primary Folder is disabled. Unable to update primary local folder")
            return
        }

        if (isInvalid(newLocalFolder)) {
            Timber.w("New local path is invalid.")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            updateBackup(
                backupId = cuSync.backupId,
                targetNode = INVALID_HANDLE,
                localFolder = newLocalFolder,
                backupName = megaApplication.getString(R.string.section_photo_sync),
                backupState = BackupState.INVALID,
            )
        }
    }

    /**
     * Update the Secondary local folder, when set to another local folder
     * This method will be called when a local folder is selected.
     *
     * @see SettingsConstants.REQUEST_LOCAL_SECONDARY_MEDIA_FOLDER
     *
     * @param newLocalFolder The path of the new Secondary local folder
     */
    fun updateSecondaryLocalFolder(newLocalFolder: String?) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            Timber.d("Secondary Folder is disabled. Unable to update secondary local folder")
            return
        }

        if (isInvalid(newLocalFolder)) {
            Timber.w("New local path is invalid.")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            updateBackup(
                backupId = muSync.backupId,
                targetNode = INVALID_HANDLE,
                localFolder = newLocalFolder,
                backupName = megaApplication.getString(R.string.section_secondary_media_uploads),
                backupState = BackupState.INVALID,
            )
        }
    }

    /**
     * Updates the Primary Folder Backup name
     *
     * @see MegaApiJava
     */
    @Deprecated(
        message = "Replace all usages with use case and pass in R.string.section_photo_sync as backupName",
        replaceWith = ReplaceWith("UpdatePrimaryFolderBackupNameUseCase")
    )
    fun updatePrimaryBackupName() {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            Timber.d("Primary Folder is disabled. Unable to update Primary Folder backup name")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync != null) {
            updateBackup(
                backupId = cuSync.backupId,
                targetNode = INVALID_HANDLE,
                localFolder = null,
                backupName = megaApplication.getString(R.string.section_photo_sync),
                backupState = BackupState.INVALID,
            )
        }
    }

    /**
     * Updates the Secondary Folder Backup name
     *
     * @see MegaApiJava
     */
    @Deprecated(
        message = "Replace all usages with use case and pass in R.string.section_secondary_media_uploads as backupName",
        replaceWith = ReplaceWith("UpdateSecondaryFolderBackupNameUseCase")
    )
    fun updateSecondaryBackupName() {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            Timber.d("Secondary Folder is disabled. Unable to update Secondary Folder backup name")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync != null) {
            updateBackup(
                backupId = muSync.backupId,
                targetNode = INVALID_HANDLE,
                localFolder = null,
                backupName = megaApplication.getString(R.string.section_secondary_media_uploads),
                backupState = BackupState.INVALID,
            )
        }
    }

    /**
     * Update the Backup State of the Primary Folder (Camera Uploads)
     *
     * @param backupState The Backup State
     */
    @Deprecated(
        message = "Replace all usages with use case and pass in R.string.section_photo_sync as backupName",
        replaceWith = ReplaceWith("UpdateCameraUploadsBackupUseCase")
    )
    fun updatePrimaryFolderBackupState(backupState: BackupState) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            Timber.d("Primary Folder is disabled. Unable to update Primary Folder backup state")
            return
        }

        val cuSync = databaseHandler.cuBackup
        if (cuSync != null && backupState != cuSync.state) {
            updateBackup(
                backupId = cuSync.backupId,
                targetNode = INVALID_HANDLE,
                localFolder = null,
                backupName = megaApplication.getString(R.string.section_photo_sync),
                backupState = backupState,
            )
        }
    }

    /**
     * Update the Backup State of the Secondary Folder
     *
     * @param backupState The Backup State
     */
    @Deprecated(
        message = "Replace all usages with use case and pass in R.string.section_secondary_media_uploads as backupName",
        replaceWith = ReplaceWith("UpdateMediaUploadsBackupUseCase")
    )
    fun updateSecondaryFolderBackupState(backupState: BackupState) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            Timber.d("Secondary Folder is disabled. Unable to update Secondary Folder backup state")
            return
        }

        val muSync = databaseHandler.muBackup
        if (muSync != null && backupState != muSync.state) {
            updateBackup(
                backupId = muSync.backupId,
                targetNode = INVALID_HANDLE,
                localFolder = null,
                backupName = megaApplication.getString(R.string.section_secondary_media_uploads),
                backupState = backupState,
            )
        }
    }

    /**
     * Update a backup.
     * For the values keep the same, pass invalid value to avoid to send to the server.
     * @see MegaApiJava.updateBackup
     *
     * @param backupId ID of the backup which is to be updated.
     * @param targetNode Handle of the MegaNode where the backup targets to.
     * @param localFolder Path of the local folder where the backup uploads from.
     * @param backupName Name of the backup. Should be "Camera Uploads" for CU and "Media Uploads" for MU.
     * @param backupState The current [BackupState]
     */
    private fun updateBackup(
        backupId: Long,
        targetNode: Long,
        localFolder: String?,
        backupName: String,
        backupState: BackupState,
    ) {
        if (isInvalid(backupId.toString())) {
            Timber.w("Invalid sync id, value: $backupId")
            return
        }
        megaApi.updateBackup(
            backupId,
            BACKUP_TYPE_INVALID,
            targetNode,
            localFolder,
            backupName,
            backupState.value,
            MegaError.API_OK,
            onBackupUpdated,
        )
    }

    /**
     * The listener for when the Backup is updated
     */
    private val onBackupUpdated =
        OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
            if (error.errorCode == MegaError.API_OK) {
                // Update local cache
                with(request) {
                    var backup = databaseHandler.getBackupById(parentHandle)
                    if (backup != null && !backup.outdated) {
                        backup =
                            backup.copy(
                                targetNode = if (nodeHandle != INVALID_HANDLE) nodeHandle else backup.targetNode,
                                localFolder = if (file != null) file else backup.localFolder,
                                state = if (access != INVALID_VALUE) BackupState.fromValue(access) else backup.state,
                                backupName = if (name != null) name else backup.backupName
                            )
                        databaseHandler.updateBackup(backup)
                        Timber.d("Successful callback: update $backup")
                    }
                }
            } else {
                Timber.w("Request ${request.type}: ${request.requestString} failed, ${error.errorString}: ${error.errorCode}")
            }
        })

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
     * Remove a Backup by its ID
     *
     * @param id the ID of the Backup to be removed
     */
    @Deprecated(
        message = "Replace all usages with use case and pass in camera upload type",
        replaceWith = ReplaceWith("RemoveBackupFolderUseCase")
    )
    private fun removeBackup(id: Long?) {
        id?.let { megaApi.removeBackup(id, onBackupRemoved) }
    }

    /**
     * The listener for when the Backup is removed
     */
    private val onBackupRemoved =
        OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
            if (error.errorCode == MegaError.API_OK) {
                // Remove local cache
                databaseHandler.deleteBackupById(request.parentHandle)
                Timber.d("Successful callback: delete ${request.parentHandle}")
            } else {
                Timber.w("Delete backup with id ${request.parentHandle} failed. Set it as outdated.")
                databaseHandler.setBackupAsOutdated(request.parentHandle)
            }
        })

    /**
     * Check if a string is invalid value, including null check and value check(whether a non-null invalid value).
     *
     * @param value String value to be checked.
     * @return true, if the string is null, empty or invalid value. false, otherwise.
     */
    private fun isInvalid(value: String?) =
        TextUtil.isTextEmpty(value) || INVALID_NON_NULL_VALUE == value

    /**
     * When the CU service has nothing to upload, send [HeartbeatStatus.UP_TO_DATE]
     * Before sending, check the account state and login again when the rootNode is null.
     * Only call in Worker class.
     */
    fun doRegularHeartbeat() {
        val isLoggingIn = MegaApplication.isLoggingIn
        if (megaApi.rootNode == null && !isLoggingIn) {
            Timber.w("RootNode = null, need to login again")
            val dbH = getDbHandler()
            val gSession = dbH.credentials?.session
            MegaApplication.isLoggingIn = true
            megaApi.fastLogin(gSession, createHeartbeatListener(onSuccess = {
                saveCredentials()
                Timber.d("CameraUploadSyncManager: fast logged in and saved session")
                megaApi.fetchNodes(createHeartbeatListener(onSuccess = {
                    MegaApplication.isLoggingIn = false
                    MegaApplication.setHeartBeatAlive(true)

                    sendPrimaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
                    sendSecondaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
                }, onError = {
                    MegaApplication.isLoggingIn = false
                }))
            }, onError = {
                MegaApplication.isLoggingIn = false
            }))
        } else {
            sendPrimaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
            sendSecondaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
        }
    }

    private fun saveCredentials() {
        val fastLoginSession = megaApi.dumpSession()
        val myUser = megaApi.myUser
        var lastEmail = ""
        var myUserHandle = ""
        if (myUser != null) {
            lastEmail = megaApi.myUser?.email.orEmpty()
            myUserHandle = megaApi.myUser?.handle.toString() + ""
        }
        val credentials =
            UserCredentials(
                lastEmail,
                fastLoginSession,
                "",
                "",
                myUserHandle
            )
        val dbH = getDbHandler()
        dbH.saveCredentials(credentials)
    }

    /**
     * The listener for Heartbeat events
     *
     * @param onSuccess Lambda that is invoked when the request is successful
     * @param onError Lambda that is invoked when the request failed
     *
     * @return [OptionalMegaRequestListenerInterface] to listen for events from [MegaApiJava.sendBackupHeartbeat]
     */
    private fun createHeartbeatListener(
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)? = null,
    ) =
        OptionalMegaRequestListenerInterface(onRequestFinish = { request, e ->
            Timber.d("${request.requestString} finished with ${e.errorCode}: ${e.errorString}")
            if (e.errorCode == MegaError.API_OK) {
                onSuccess?.invoke()
            } else {
                Timber.e("${request.requestString} failed with ${e.errorCode}: ${e.errorString}")
                onError?.invoke()
            }
        })

    /**
     * Send a broadcast to re-enable CU/MU preference in settings fragment.
     * In order to prevent user enable/disable CU/MU too fast,
     * after enabled CU/MU, the corresponding preference in settings fragment will be set as disabled.
     * After setBackup finished, need to re-enable the preference.
     *
     * @param which Re-enable which preference, CU or MU.
     */
    private fun reEnableCameraUploadsPreference(which: Int) = MegaApplication.getInstance()
        .sendBroadcast(
            Intent(BROADCAST_ACTION_REENABLE_CU_PREFERENCE).putExtra(
                KEY_REENABLE_WHICH_PREFERENCE,
                which
            ).setPackage(megaApplication.applicationContext.packageName)
        )
}
