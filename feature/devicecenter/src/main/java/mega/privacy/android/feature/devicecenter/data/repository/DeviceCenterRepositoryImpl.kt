package mega.privacy.android.feature.devicecenter.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.devicecenter.domain.exception.SetDeviceNameException
import mega.privacy.android.feature.devicecenter.domain.repository.DeviceCenterRepository
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [DeviceCenterRepository]
 *
 * @property ioDispatcher [CoroutineDispatcher]
 * @property megaApiGateway [MegaApiGateway]
 */
internal class DeviceCenterRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiGateway: MegaApiGateway,
) : DeviceCenterRepository {
    override suspend fun setDeviceName(deviceName: String) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    Timber.d("setDeviceName() - MegaRequest.${request.name}, MegaError.${error.errorString}")
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            continuation.resumeWith(Result.success(Unit))
                        }

                        MegaError.API_EEXIST -> {
                            continuation.failWithException(
                                SetDeviceNameException.NameAlreadyExists(
                                    errorCode = error.errorCode,
                                    errorString = error.errorString,
                                )
                            )
                        }

                        else -> {
                            continuation.failWithException(
                                SetDeviceNameException.Unknown(
                                    errorCode = error.errorCode,
                                    errorString = error.errorString,
                                )
                            )
                        }
                    }
                }
            )
            megaApiGateway.setDeviceName(
                deviceName = deviceName,
                listener = listener,
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getDeviceId() = megaApiGateway.getDeviceId()
}