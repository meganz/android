package mega.privacy.android.feature.devicecenter.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.devicecenter.data.mapper.DeviceNodeMapper
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import javax.inject.Inject

/**
 * Default implementation of [DeviceCenterRepository]
 *
 * @property deviceNodeMapper [DeviceNodeMapper]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property megaApiGateway [MegaApiGateway]
 */
internal class DeviceCenterRepositoryImpl @Inject constructor(
    private val deviceNodeMapper: DeviceNodeMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : DeviceCenterRepository {

    override suspend fun getDevices(
        backupInfoList: List<BackupInfo>,
        currentDeviceId: String,
        deviceIdAndNameMap: Map<String, String>,
        isCameraUploadsEnabled: Boolean,
    ) = withContext(ioDispatcher) {
        deviceNodeMapper(
            backupInfoList = backupInfoList,
            currentDeviceId = currentDeviceId,
            deviceIdAndNameMap = deviceIdAndNameMap,
            isCameraUploadsEnabled = isCameraUploadsEnabled,
        )
    }
}
