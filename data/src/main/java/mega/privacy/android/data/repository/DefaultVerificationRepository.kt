package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.TelephonyGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.CountryCallingCodeMapper
import mega.privacy.android.data.mapper.verification.SmsPermissionMapper
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.exception.SMSVerificationException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VerificationRepository
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Default verification repository
 *
 * @property megaApiGateway
 * @property ioDispatcher
 * @property appEventGateway
 * @property telephonyGateway
 * @property countryCallingCodeMapper
 * @property smsPermissionMapper
 * @property appScope
 */
internal class DefaultVerificationRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
    private val telephonyGateway: TelephonyGateway,
    private val countryCallingCodeMapper: CountryCallingCodeMapper,
    private val smsPermissionMapper: SmsPermissionMapper,
    @ApplicationScope private val appScope: CoroutineScope,
) : VerificationRepository {

    private val verifiedPhoneNumberFlow = MutableSharedFlow<VerifiedPhoneNumber>(replay = 1)

    override suspend fun setSMSVerificationShown(isShown: Boolean) =
        appEventGateway.setSMSVerificationShown(isShown)

    override suspend fun isSMSVerificationShown() = appEventGateway.isSMSVerificationShown()

    override suspend fun getCountryCallingCodes() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("getCountryCallingCodes") {
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
                                continuation.failWithException(
                                    SMSVerificationException.LimitReached(
                                        error.errorCode,
                                    )
                                )
                            }
                            MegaError.API_EACCESS -> {
                                continuation.failWithException(
                                    SMSVerificationException.AlreadyVerified(
                                        error.errorCode,
                                    )
                                )
                            }
                            MegaError.API_EARGS -> {
                                continuation.failWithException(
                                    SMSVerificationException.InvalidPhoneNumber(
                                        error.errorCode,
                                    )
                                )
                            }
                            MegaError.API_EEXIST -> {
                                continuation.failWithException(
                                    SMSVerificationException.AlreadyExists(
                                        error.errorCode,
                                    )
                                )
                            }
                            else -> {
                                continuation.failWithException(
                                    SMSVerificationException.Unknown(
                                        error.errorCode,
                                    )
                                )
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

    override suspend fun resetSMSVerifiedPhoneNumber() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK || error.errorCode == MegaError.API_ENOENT) {
                        updateVerifiedPhoneNumber()
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        Timber.e("Calling resetSMSVerifiedPhoneNumber failed with error code ${error.errorCode}")
                        continuation.failWithError(error, "resetSMSVerifiedPhoneNumber")
                    }
                }
            )

            megaApiGateway.resetSmsVerifiedPhoneNumber(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    private fun updateVerifiedPhoneNumber() {
        appScope.launch {
            verifiedPhoneNumberFlow.emit(getVerifiedPhoneNumber())
        }
    }

    override suspend fun getCurrentCountryCode() = telephonyGateway.getCurrentCountryCode()

    override suspend fun isRoaming() = telephonyGateway.isRoaming()

    override suspend fun formatPhoneNumber(number: String, countryCode: String) =
        telephonyGateway.formatPhoneNumber(number, countryCode)

    override fun monitorVerifiedPhoneNumber() = flow {
        emit(getVerifiedPhoneNumber())
        emitAll(verifiedPhoneNumberFlow)
    }

    private suspend fun getVerifiedPhoneNumber() =
        megaApiGateway.getVerifiedPhoneNumber()?.let { VerifiedPhoneNumber.PhoneNumber(it) }
            ?: VerifiedPhoneNumber.NoVerifiedPhoneNumber

    override suspend fun verifyPhoneNumber(pin: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    updateVerifiedPhoneNumber()
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(Unit))
                        }
                        MegaError.API_EACCESS -> {
                            continuation.failWithException(
                                SMSVerificationException.LimitReached(
                                    error.errorCode,
                                )
                            )
                        }
                        MegaError.API_EEXPIRED -> {
                            continuation.failWithException(
                                SMSVerificationException.AlreadyVerified(
                                    error.errorCode,
                                )
                            )
                        }
                        MegaError.API_EEXIST -> {
                            continuation.failWithException(
                                SMSVerificationException.AlreadyExists(
                                    error.errorCode,
                                )
                            )
                        }
                        MegaError.API_EFAILED -> {
                            continuation.failWithException(
                                SMSVerificationException.VerificationCodeDoesNotMatch(
                                    error.errorCode,
                                    error.errorString
                                )
                            )
                        }
                        else -> {
                            continuation.failWithException(
                                SMSVerificationException.Unknown(
                                    error.errorCode,
                                )
                            )
                        }
                    }
                }
            )

            megaApiGateway.verifyPhoneNumber(pin, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getSmsPermissions() =
        withContext(ioDispatcher) { smsPermissionMapper(megaApiGateway.getSmsAllowedState()) }

}
