package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.listeners.OptionalMegaChatListenerInterface
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatPresenceConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to subscribe to global events related to MegaChat.
 *
 * @property megaChatApi    MegaChatApi required to call the SDK
 */
@Singleton
class GetChatChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    sealed class Result {
        data class OnChatListItemUpdate(val item: MegaChatListItem?) : Result()
        data class OnChatInitStateUpdate(val newState: Int) : Result()
        data class OnChatOnlineStatusUpdate(val userHandle: Long, val status: Int, val inProgress: Boolean) : Result()
        data class OnChatPresenceConfigUpdate(val config: MegaChatPresenceConfig) : Result()
        data class OnChatConnectionStateUpdate(val chatid: Long, val newState: Int) : Result()
        data class OnChatPresenceLastGreen(val userHandle: Long, val lastGreen: Int) : Result()
        data class OnDbError(val error: Int, val msg: String) : Result()
    }

    fun get(): Flowable<Result> =
        Flowable.create({ emitter ->
            val listener = OptionalMegaChatListenerInterface(
                onChatListItemUpdate = { item ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnChatListItemUpdate(item))
                    }
                },
                onChatInitStateUpdate = { newState ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnChatInitStateUpdate(newState))
                    }
                },
                onChatOnlineStatusUpdate = { userHandle, status, inProgress ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnChatOnlineStatusUpdate(userHandle, status, inProgress))
                    }
                },
                onChatPresenceConfigUpdate = { config ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnChatPresenceConfigUpdate(config))
                    }
                },
                onChatConnectionStateUpdate = { chatid, newState ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnChatConnectionStateUpdate(chatid, newState))
                    }
                },
                onChatPresenceLastGreen = { userHandle, lastGreen ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnChatPresenceLastGreen(userHandle, lastGreen))
                    }
                },
                onDbError = { error, msg ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnDbError(error, msg))
                    }
                }
            )

            megaChatApi.addChatListener(listener)

            emitter.setCancellable {
                megaChatApi.removeChatListener(listener)
            }
        }, BackpressureStrategy.BUFFER)
}
