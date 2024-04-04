package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SupportTicket
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Create support ticket
 *
 */
class CreateSupportTicketUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {
    suspend operator fun invoke(
        description: String,
        logFileName: String?,
        accountDetails: UserAccount?,
    ): SupportTicket {
        val (appVersion, sdkVersion) = environmentRepository.getAppInfo()
        val (device, languageCode) = environmentRepository.getDeviceInfo()

        return SupportTicket(
            androidAppVersion = appVersion,
            sdkVersion = sdkVersion,
            device = device,
            currentLanguage = languageCode,
            accountEmail = accountDetails?.email ?: "Unknown",
            accountType = accountDetails?.accountTypeString ?: "Unknown",
            description = description,
            logFileName = logFileName,
            deviceSdkVersionInt = environmentRepository.getDeviceSdkVersionInt(),
            deviceSdkVersionName = environmentRepository.getDeviceSdkVersionName()
        )
    }
}