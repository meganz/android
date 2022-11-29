package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.repository.SMSVerificationRepository
import javax.inject.Inject

internal class DefaultSMSVerificationRepository @Inject constructor(private val appEventGateway: AppEventGateway) :
    SMSVerificationRepository {
    override suspend fun setSMSVerificationShown(isShown: Boolean) =
        appEventGateway.setSMSVerificationShown(isShown)

    override suspend fun isSMSVerificationShown() = appEventGateway.isSMSVerificationShown()
}
