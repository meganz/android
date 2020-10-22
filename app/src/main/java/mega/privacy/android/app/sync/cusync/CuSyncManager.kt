package mega.privacy.android.app.sync.cusync

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop.INVALID_NON_NULL_VALUE
import mega.privacy.android.app.sync.SyncListener
import mega.privacy.android.app.sync.cusync.callback.RemoveBackupCallback
import mega.privacy.android.app.sync.cusync.callback.SetBackupCallback
import mega.privacy.android.app.sync.cusync.callback.UpdateBackupCallback
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava


class CuSyncManager {

    private val megaApplication: MegaApplication = MegaApplication.getInstance()

    private val megaApiJava: MegaApiAndroid = megaApplication.megaApi

    private val databaseHandler: DatabaseHandler = DatabaseHandler.getDbHandler(megaApplication)

    companion object {
        const val TYPE_BACKUP_PRIMARY = 0
        const val TYPE_BACKUP_SECONDARY = 1
        const val NAME_PRIMARY = "camera uploads"
        const val NAME_SECONDARY = "media uploads"
        const val NAME_OTHER = "other sync"
        const val DEVICE_ID = "Ash's phone"
    }

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
        state: Int = MegaApiJava.STATE_PENDING,
        subState: Int = MegaApiJava.STATE_PENDING,
        extraData: String = ""
    ) {
        if (isInvalid(targetNode?.toString())) {
            logWarning("Target handle is invalid, value: $targetNode")
            return
        }

        if (isInvalid(localFolder)) {
            logWarning("Local path is invalid, value: $localFolder")
            return
        }

        megaApiJava.setBackup(
            backupType,
            targetNode!!,
            localFolder,
            DEVICE_ID,
            state,
            subState,
            extraData,
            SyncListener(SetBackupCallback(), megaApplication)
        )
    }

    fun updatePrimaryTargetNode(newTargetNode: Long?) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        if (isInvalid(newTargetNode?.toString())) {
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
                    newTargetNode!!,
                    localFolder,
                    deviceId,
                    state,
                    subState,
                    extraData
                )
            }
        }
    }

    fun updateSecondaryTargetNode(newTargetNode: Long?) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled.")
            return
        }

        if (isInvalid(newTargetNode?.toString())) {
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
                    newTargetNode!!,
                    localFolder,
                    deviceId,
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
                    deviceId,
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
                    deviceId,
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

        megaApiJava.updateBackup(
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
            megaApiJava.removeBackup(id, SyncListener(RemoveBackupCallback(), megaApplication))
        }
    }

    private fun isInvalid(value: String?) =
        TextUtil.isTextEmpty(value) || INVALID_NON_NULL_VALUE == value
}