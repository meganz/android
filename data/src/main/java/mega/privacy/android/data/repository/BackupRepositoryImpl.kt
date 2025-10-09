package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.backup.BackupDeviceNamesMapper
import mega.privacy.android.data.mapper.backup.BackupInfoListMapper
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.data.mapper.backup.BackupMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BackupRepository
import mega.privacy.android.domain.repository.BackupRepository.Companion.BACKUPS_FOLDER_DEFAULT_NAME
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [BackupRepository]
 *
 * @property backupDeviceNamesMapper [BackupDeviceNamesMapper]
 * @property backupInfoListMapper [BackupInfoListMapper]
 * @property backupMapper [BackupMapper]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property megaApiGateway [MegaApiGateway]
 * @property appEventGateway [AppEventGateway]
 * @property megaLocalRoomGateway [MegaLocalRoomGateway]
 * @property backupInfoTypeIntMapper [BackupInfoTypeIntMapper]
 * @property backupStateIntMapper [BackupStateIntMapper]
 */
internal class BackupRepositoryImpl @Inject constructor(
    private val backupDeviceNamesMapper: BackupDeviceNamesMapper,
    private val backupInfoListMapper: BackupInfoListMapper,
    private val backupMapper: BackupMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
    private val appEventGateway: AppEventGateway,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper,
    private val backupStateIntMapper: BackupStateIntMapper,
) : BackupRepository {
    override suspend fun getDeviceId() = withContext(ioDispatcher) {
        megaApiGateway.getDeviceId()
    }

    override suspend fun getDeviceIdAndNameMap() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getDeviceIdAndNameMap") {
                backupDeviceNamesMapper(it.megaStringMap)
            }
            megaApiGateway.getUserAttribute(
                attributeIdentifier = MegaApiJava.USER_ATTR_DEVICE_NAMES,
                listener = listener,
            )

        }
    }

    override suspend fun getDeviceName(deviceId: String): String? = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getDeviceName") {
                it.name
            }
            megaApiGateway.getDeviceName(
                deviceId = deviceId,
                listener = listener,
            )
        }
    }

    override suspend fun renameDevice(deviceId: String, deviceName: String) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("renameDevice") {
                    return@getRequestListener
                }
                megaApiGateway.setDeviceName(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    listener = listener,
                )
            }
        }

    override suspend fun getBackupInfo() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getBackupInfo") {
                it.megaBackupInfoList
            }
            megaApiGateway.getBackupInfo(listener)
        }.let {
            backupInfoListMapper(it)
        }
    }

    override suspend fun setBackup(
        backupType: BackupInfoType,
        targetNode: Long,
        localFolder: String,
        backupName: String,
        state: BackupState,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setBackup") {
                Timber.d("setBackup $it")
                backupMapper(it)
            }
            megaApiGateway.setBackup(
                backupType = backupInfoTypeIntMapper(backupType),
                targetNode = targetNode,
                localFolder = localFolder,
                backupName = backupName,
                state = backupStateIntMapper(state),
                subState = MegaError.API_OK,
                listener = listener
            )
        }
    }

    override suspend fun updateRemoteBackup(
        backupId: Long,
        backupType: BackupInfoType,
        backupName: String,
        targetNode: Long,
        localFolder: String?,
        state: BackupState,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("updateRemoteBackupName") {
                return@getRequestListener
            }
            // Any values that should not be changed should be marked as null, -1 or -1L
            megaApiGateway.updateBackup(
                backupId = backupId,
                backupType = backupInfoTypeIntMapper(backupType),
                targetNode = targetNode,
                localFolder = null,
                backupName = backupName,
                state = backupStateIntMapper(state),
                subState = MegaError.API_OK,
                listener = listener,
            )
        }
    }

    override fun monitorBackupInfoType() = appEventGateway.monitorBackupInfoType()

    override suspend fun broadCastBackupInfoType(backupInfoType: BackupInfoType) {
        appEventGateway.broadCastBackupInfoType(backupInfoType)
    }

    override suspend fun saveBackup(backup: Backup) = withContext(ioDispatcher) {
        megaLocalRoomGateway.saveBackup(backup).also {
            Timber.d("Local Backup saved $backup")
        }
    }

    override suspend fun myBackupsFolderExists(): Boolean =
        suspendCancellableCoroutine { continuation ->
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request: MegaRequest, error: MegaError ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(request.megaStringMap != null))
                    } else {
                        continuation.resumeWith(Result.success(false))
                    }
                })
            megaApiGateway.getUserAttribute(
                attributeIdentifier = MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER,
                listener = listener,
            )
        }

    override suspend fun setMyBackupsFolder(localizedName: String?): NodeId =
        suspendCancellableCoroutine { continuation ->
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK, MegaError.API_EEXIST -> {
                            continuation.resumeWith(Result.success(NodeId(request.nodeHandle)))
                        }

                        else -> {
                            continuation.failWithError(error, "setMyBackupsFolder")
                        }
                    }
                })
            megaApiGateway.setMyBackupsFolder(
                localizedName = localizedName ?: BACKUPS_FOLDER_DEFAULT_NAME,
                listener = listener,
            )
        }
}
