package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

class GetChatRoomUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    fun get(userHandle: Long): Single<Long> =
        Single.create { emitter ->
            val chat = megaChatApi.getChatRoomByUser(userHandle)

            if (chat != null) {
                emitter.onSuccess(chat.chatId)
            } else {
                val chatPeers = MegaChatPeerList.createInstance().apply {
                    addPeer(userHandle, MegaChatPeerList.PRIV_STANDARD)
                }

                megaChatApi.createChat(false, chatPeers, OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                        if (emitter.isDisposed) return@OptionalMegaChatRequestListenerInterface

                        if (error.errorCode == API_OK) {
                            emitter.onSuccess(request.chatHandle)
                        } else {
                            emitter.onError(error.toThrowable())
                        }
                    },
                    onRequestTemporaryError = { _: MegaChatRequest, error: MegaChatError ->
                        logError(error.toThrowable().stackTraceToString())
                    }
                ))
            }
        }
}
