package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

internal class DefaultVerificationRepository @Inject constructor(private val appEventGateway: AppEventGateway) :
    VerificationRepository {
    override suspend fun setSMSVerificationShown(isShown: Boolean) =
        appEventGateway.setSMSVerificationShown(isShown)

    override suspend fun isSMSVerificationShown() = appEventGateway.isSMSVerificationShown()
}
