package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case to archive chat
 *
 * @property megaChatApi    Needed to retrieve chat list items
 */
class ArchiveChatUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Archive/unarchive specific chat
     *
     * @param chatId    MegaChatHandle that identifies the chat room
     * @param archive   True to set the chat as archived, false to unarchive it.
     * @return          Completable
     */
    fun archive(chatId: Long, archive: Boolean): Completable =
        Completable.create { emitter ->
            megaChatApi.archiveChat(chatId, archive, OptionalMegaChatRequestListenerInterface(
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
