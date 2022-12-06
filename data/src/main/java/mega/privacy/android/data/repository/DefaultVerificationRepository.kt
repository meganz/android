package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.domain.exception.SMSVerificationException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VerificationRepository
import nz.mega.sdk.MegaError
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

    override suspend fun sendSMSVerificationCode(phoneNumber: String): Unit =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                continuation.resumeWith(Result.success(Unit))
                            }
                            MegaError.API_ETEMPUNAVAIL -> {
                                continuation.failWithException(SMSVerificationException.LimitReached(
                                    error.errorCode))
                            }
                            MegaError.API_EACCESS -> {
                                continuation.failWithException(SMSVerificationException.AlreadyVerified(
                                    error.errorCode))
                            }
                            MegaError.API_EARGS -> {
                                continuation.failWithException(SMSVerificationException.InvalidPhoneNumber(
                                    error.errorCode))
                            }
                            MegaError.API_EEXIST -> {
                                continuation.failWithException(SMSVerificationException.AlreadyExists(
                                    error.errorCode))
                            }
                            else -> {
                                continuation.failWithException(SMSVerificationException.Unknown(
                                    error.errorCode))
                            }
                        }
                    }
                )
                megaApiGateway.sendSMSVerificationCode(
                    phoneNumber,
                    listener = listener,
                    reVerifyingWhitelisted = false,
                )
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
}
