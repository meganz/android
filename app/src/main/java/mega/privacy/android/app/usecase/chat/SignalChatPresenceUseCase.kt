package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case to signal chat presence
 *
 * @property megaChatApi    MegaChatApi required to signal chat presence
 */
class SignalChatPresenceUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Signal chat presence if required
     *
     * @return  Completable
     */
    fun signal(): Completable =
        Completable.create { emitter ->
            if (megaChatApi.isSignalActivityRequired) {
                megaChatApi.signalPresenceActivity(OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        when {
                            emitter.isDisposed -> return@OptionalMegaChatRequestListenerInterface
                            error.errorCode == MegaError.API_OK -> emitter.onComplete()
                            else -> emitter.onError(error.toMegaException())
                        }
                    }
                ))
            } else {
                emitter.onComplete()
            }
        }
}
