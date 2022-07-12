package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.data.gateway.DeviceGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.DeviceInfo
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * [EnvironmentRepository] Implementation
 *
 */
class DefaultEnvironmentRepository @Inject constructor(
    private val deviceGateway: DeviceGateway,
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : EnvironmentRepository {

    override fun getDeviceInfo(): DeviceInfo {
        val deviceName = getDeviceName()

        return DeviceInfo(deviceName, getLanguage())
    }

    private fun getDeviceName(): String {
        val manufacturer = deviceGateway.getManufacturerName().run {
            if (this.equals("HTC", true)) uppercase() else this
        }
        val model = deviceGateway.getDeviceModel()
        return "$manufacturer $model"
    }

    private fun getLanguage(): String = deviceGateway.getCurrentDeviceLanguage()

    override suspend fun getAppInfo(): AppInfo = withContext(ioDispatcher) {
        AppInfo(
            appVersion = context.getString(R.string.app_version),
            sdkVersion = megaApiGateway.getSdkVersion(),
        )
    }

}
