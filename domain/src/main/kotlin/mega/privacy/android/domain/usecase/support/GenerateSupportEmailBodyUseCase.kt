package mega.privacy.android.domain.usecase.support

import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use case to generate the body of the support email
 */
class GenerateSupportEmailBodyUseCase @Inject constructor(
    private val deviceRepository: EnvironmentRepository,
    private val networkRepository: NetworkRepository,
) {

    /**
     * Generate the body of the support email
     */
    suspend operator fun invoke(): String {
        val appInfo = deviceRepository.getAppInfo()
        val deviceInfo = deviceRepository.getDeviceInfo()
        val stringBuilder = StringBuilder()
            .append("Please write your feedback here:")
            .append("\n\n\n\n\n\n\n\n\n\n\n")
            .append("App information:")
            .append("\n")
            .append("App Name: MEGA").append("\n")
            .append("App Version: ").append(appInfo.appVersion).append("\n\n")
            .append("Device information:")
            .append("\n")
            .append("Device: ").append(deviceInfo.device).append("\n")
            .append("Android version: ").append(deviceRepository.getDeviceSdkVersionName()).append("\n")
            .append("Language: ").append(deviceInfo.language).append("\n")
            .append("Timezone: ").append(deviceRepository.getTimezone()).append("\n")
            .append("Connection Status: ").append(getConnectionStatus()).append("\n")
        return stringBuilder.toString()
    }

    private fun getConnectionStatus(): String {
        return when (val state = networkRepository.getCurrentConnectivityState()) {
            ConnectivityState.Disconnected -> "NOT NETWORK"
            is ConnectivityState.Connected -> if (state.isOnWifi) "WIFI" else "MOBILE"
        }
    }
}