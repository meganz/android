package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatError
import javax.inject.Inject

/**
 * Main use case to forward a Mega Chat Message.
 *
 * @property megaChatApi    Mega Chat API needed to get message information.
 */
class ForwardChatMessageUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    fun attach(toChatRoomId: Long, nodeHandle: Long): Completable =
        Completable.create { emitter ->
            megaChatApi.attachNode(toChatRoomId, nodeHandle, OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaChatError.ERROR_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}
