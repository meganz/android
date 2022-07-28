package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case to leave chat
 *
 * @property megaChatApi    Needed to retrieve chat list items
 */
class LeaveChatUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Leave a chatroom
     *
     * @param chatId    MegaChatHandle that identifies the chat room
     * @return          Completable
     */
    fun leave(chatId: Long): Completable =
        Completable.create { emitter ->
            megaChatApi.leaveChat(chatId, OptionalMegaChatRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when {
                        emitter.isDisposed -> return@OptionalMegaChatRequestListenerInterface
                        error.errorCode == MegaError.API_OK -> emitter.onComplete()
                        else -> emitter.onError(error.toMegaException())
                    }
                }
            ))
        }
}
