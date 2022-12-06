package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VerificationRepository
import javax.inject.Inject

internal class DefaultVerificationRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
    private val countryCallingCodeMapper: CountryCallingCodeMapper,
) : VerificationRepository {
    override suspend fun setSMSVerificationShown(isShown: Boolean) =
        appEventGateway.setSMSVerificationShown(isShown)

    override suspend fun isSMSVerificationShown() = appEventGateway.isSMSVerificationShown()

    override suspend fun getCountryCallingCodes() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener {
                return@getRequestListener countryCallingCodeMapper(it.megaStringListMap)
            }
            megaApiGateway.getCountryCallingCodes(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }
}
