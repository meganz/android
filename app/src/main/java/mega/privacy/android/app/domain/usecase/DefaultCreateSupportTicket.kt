package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.SupportTicket
import mega.privacy.android.app.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Default create support ticket
 *
 * @property environmentRepository
 * @property getAccountDetails
 */
class DefaultCreateSupportTicket @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
    private val getAccountDetails: GetAccountDetails,
) : CreateSupportTicket {
    override suspend fun invoke(
        description: String,
        logFileName: String?,
    ): SupportTicket {
        val (appVersion, sdkVersion) = environmentRepository.getAppInfo()
        val (device, languageCode) = environmentRepository.getDeviceInfo()
        val accountDetails = getAccountDetails(false)

        return SupportTicket(
            androidAppVersion = appVersion,
            sdkVersion = sdkVersion,
            device = device,
            currentLanguage = languageCode,
            accountEmail = accountDetails.email,
            accountType = accountDetails.accountTypeString,
            description = description,
            logFileName = logFileName
        )
    }
}