package mega.privacy.android.feature.devicecenter.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.devicecenter.data.mapper.BackupDeviceNamesMapper
import mega.privacy.android.feature.devicecenter.data.mapper.BackupInfoListMapper
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Default implementation of [DeviceCenterRepository]
 *
 * @property backupDeviceNamesMapper [BackupDeviceNamesMapper]
 * @property backupInfoListMapper [BackupInfoListMapper]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property megaApiGateway [MegaApiGateway]
 */
internal class DeviceCenterRepositoryImpl @Inject constructor(
    private val backupDeviceNamesMapper: BackupDeviceNamesMapper,
    private val backupInfoListMapper: BackupInfoListMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : DeviceCenterRepository {
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
}