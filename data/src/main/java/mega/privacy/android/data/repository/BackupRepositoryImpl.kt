package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.backup.BackupDeviceNamesMapper
import mega.privacy.android.data.mapper.backup.BackupInfoListMapper
import mega.privacy.android.data.mapper.backup.BackupMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BackupRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Default implementation of [BackupRepository]
 *
 * @property backupDeviceNamesMapper [BackupDeviceNamesMapper]
 * @property backupInfoListMapper [BackupInfoListMapper]
 * @property backupMapper [BackupMapper]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property megaApiGateway [MegaApiGateway]
 */
internal class BackupRepositoryImpl @Inject constructor(
    private val backupDeviceNamesMapper: BackupDeviceNamesMapper,
    private val backupInfoListMapper: BackupInfoListMapper,
    private val backupMapper: BackupMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
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
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
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
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
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
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun getBackupInfo() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getBackupInfo") {
                backupInfoListMapper(it.megaBackupInfoList)
            }
            megaApiGateway.getBackupInfo(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun setBackup(
        backupType: Int,
        targetNode: Long,
        localFolder: String,
        backupName: String,
        state: Int,
        subState: Int,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("setBackup") {
                backupMapper(it)
            }
            megaApiGateway.setBackup(
                backupType = backupType,
                targetNode = targetNode,
                localFolder = localFolder,
                backupName = backupName,
                state = state,
                subState = subState,
                listener = listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }
}
