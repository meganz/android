package mega.privacy.android.app.sync.cusync

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop.INVALID_NON_NULL_VALUE
import mega.privacy.android.app.sync.SyncListener
import mega.privacy.android.app.sync.cusync.callback.DeleteBackupCallback
import mega.privacy.android.app.sync.cusync.callback.SetBackupCallback
import mega.privacy.android.app.sync.cusync.callback.UpdateSyncCallback
import mega.privacy.android.app.sync.mock.*
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiAndroid


class CuSyncManager {

    private val megaApplication: MegaApplication = MegaApplication.getInstance()

    private val megaApiJava: MegaApiAndroid = megaApplication.megaApi

    private val listener = SyncListener(megaApplication)

    private val databaseHandler: DatabaseHandler = DatabaseHandler.getDbHandler(megaApplication)

    fun setPrimaryBackup() =
        setBackup(
            SYNC_TYPE_PRIMARY,
            databaseHandler.preferences?.camSyncHandle?.toLong(),
            databaseHandler.preferences?.camSyncLocalPath
        )


    fun setSecondaryBackup() =
        setBackup(
            SYNC_TYPE_SECONDARY,
            databaseHandler.preferences?.megaHandleSecondaryFolder?.toLong(),
            databaseHandler.preferences?.localPathSecondaryFolder
        )


    private fun setBackup(
        backupType: Int,
        targetHandle: Long?,
        localFolderPath: String?,
        state: Int? = 0,
        subState: Int? = 0,
        extraData: String? = INVALID_NON_NULL_VALUE
    ) {
        if (isInvalid(targetHandle?.toString())) {
            logWarning("Target handle is invalid, value: $targetHandle")
            return
        }

        if (isInvalid(localFolderPath)) {
            logWarning("Local path is invalid, value: $localFolderPath")
            return
        }

        listener.callback = SetBackupCallback()
        val name = when (backupType) {
            SYNC_TYPE_PRIMARY -> NAME_PRIMARY
            SYNC_TYPE_SECONDARY -> NAME_SECONDARY
            else -> NAME_OTHER
        }

        // TODO SDK call
        setBackupMock(backupType, targetHandle!!, localFolderPath!!, name, listener)
    }

    fun updatePrimaryTargetHandle(newHandle: Long?) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        val cuSync = databaseHandler.cuSyncPair
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            updateBackup(cuSync.syncId, newHandle, cuSync.localFolderPath)
        }
    }

    fun updateSecondaryTargetHandle(newHandle: Long?) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled.")
            return
        }

        val muSync = databaseHandler.muSyncPair
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            updateBackup(muSync.syncId, newHandle, muSync.localFolderPath)
        }
    }

    fun updatePrimaryLocalPath(newPath: String?) {
        if (!CameraUploadUtil.isPrimaryEnabled()) {
            logDebug("CU is not enabled.")
            return
        }

        val cuSync = databaseHandler.cuSyncPair
        if (cuSync == null) {
            setPrimaryBackup()
        } else {
            updateBackup(cuSync.syncId, cuSync.targetFodlerHanlde, newPath)
        }
    }

    fun updateSecondaryLocalPath(newPath: String?) {
        if (!CameraUploadUtil.isSecondaryEnabled()) {
            logDebug("MU is not enabled.")
            return
        }

        val muSync = databaseHandler.muSyncPair
        if (muSync == null) {
            setSecondaryBackup()
        } else {
            updateBackup(muSync.syncId, muSync.targetFodlerHanlde, newPath)
        }
    }

    private fun updateBackup(
        backupId: Long?,
        targetHandle: Long?,
        localFolderPath: String?
    ) {
        if (isInvalid(backupId?.toString())) {
            logWarning("Invalid sync id, value: $backupId")
            return
        }

        if (isInvalid(targetHandle?.toString())) {
            logWarning("Invalid target handle, value: $targetHandle")
            return
        }

        if (isInvalid(localFolderPath)) {
            logWarning("New local path is invalid, value: $localFolderPath")
            return
        }

        listener.callback = UpdateSyncCallback()
        // TODO SDK call
        updateBackupMock(backupId!!, targetHandle!!, localFolderPath!!, listener)
    }

    fun deletePrimaryBackup() {
        deleteBackup(databaseHandler.cuSyncPair?.syncId)
    }

    fun deleteSecondaryBackup() {
        deleteBackup(databaseHandler.muSyncPair?.syncId)
    }

    private fun deleteBackup(id: Long?) {
        listener.callback = DeleteBackupCallback()
        // TODO SDK call
        id?.let {
            deleteBackupMock(id, listener)
        }
    }

    private fun isInvalid(value: String?) =
        TextUtil.isTextEmpty(value) || INVALID_NON_NULL_VALUE == value
}