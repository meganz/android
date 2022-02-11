package mega.privacy.android.app.usecase.chat

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val megaChatApi: MegaChatApiAndroid
) {

    fun attach(chatRoomId: Long, nodeHandle: Long): Completable =
        Completable.create { emitter ->
            megaChatApi.attachNode(chatRoomId, nodeHandle, OptionalMegaChatRequestListenerInterface(
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
