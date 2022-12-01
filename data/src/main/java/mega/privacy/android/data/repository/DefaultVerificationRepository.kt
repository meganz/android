package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.VerificationRepository
import nz.mega.sdk.MegaError
import javax.inject.Inject

internal class DefaultVerificationRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
) :
    VerificationRepository {
    override suspend fun setSMSVerificationShown(isShown: Boolean) =
        appEventGateway.setSMSVerificationShown(isShown)

    override suspend fun isSMSVerificationShown() = appEventGateway.isSMSVerificationShown()

    override suspend fun getCountryCallingCodes() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val codedCountryCode = mutableListOf<String>()
                        val listMap = request.megaStringListMap
                        val keyList = listMap.keys
                        for (i in 0 until keyList.size()) {
                            val key = keyList[i]
                            val contentBuffer = StringBuffer()
                            contentBuffer.append("$key:")
                            for (j in 0 until listMap[key].size()) {
                                val dialCode = listMap[key][j]
                                contentBuffer.append("$dialCode,")
                            }
                            codedCountryCode.add(contentBuffer.toString())
                        }
                        continuation.resumeWith(Result.success(codedCountryCode))
                    } else {
                        continuation.failWithError(error)
                    }
                }
            )
            megaApiGateway.getCountryCallingCodes(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }
}
