package mega.privacy.android.app.presentation.verification.usecase

import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH_PHONE_NUMBER
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

@Deprecated(
    "This use case is being replaced with a domain implementation. Also see [AppFeatures.MonitorPhoneNumber]",
    replaceWith = ReplaceWith("mega.privacy.android.domain.usecase.verification.ResetSMSVerifiedPhoneNumber"),
    level = DeprecationLevel.WARNING
)
class ResetPhoneNumberUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Launches a request to remove current verified phone number.
     */
    suspend operator fun invoke() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                when (error.errorCode) {
                    API_OK, API_ENOENT -> {
                        LiveEventBus.get(EVENT_REFRESH_PHONE_NUMBER, Boolean::class.java)
                            .post(true)
                        continuation.resumeWith(Result.success(Unit))
                    }
                    else -> continuation.failWithError(error)
                }
            })
            megaApi.resetSmsVerifiedPhoneNumber(listener)

            continuation.invokeOnCancellation {
                megaApi.removeRequestListener(listener)
            }
        }
    }
}