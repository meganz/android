package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case to check chat DND notification status
 *
 * @property megaApi    Needed to retrieve push notification settings
 */
class CheckChatDndUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    /**
     * Check if chat DND is enabled
     *
     * @param chatId    MegaChatHandle that identifies the chat room
     * @return          Completable
     */
    fun check(chatId: Long): Single<Boolean> =
        Single.create { emitter ->
            megaApi.getPushNotificationSettings(OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when {
                        emitter.isDisposed ->
                            return@OptionalMegaRequestListenerInterface
                        error.errorCode == MegaError.API_OK ->
                            emitter.onSuccess(request.megaPushNotificationSettings.isChatDndEnabled(chatId))
                        else ->
                            emitter.onError(error.toMegaException())
                    }

                }
            ))
        }
}
