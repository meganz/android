package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Use Case to remove contact, delete its inshares and hang up current calls
 */
class RemoveContactUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    fun remove(contact: MegaUser): Completable =
        Completable.create { emitter ->
            removeInShares(contact).blockingSubscribeBy(onError = emitter::onError)
            hangCurrentCallIfNeeded(contact.handle).blockingSubscribeBy(onError = emitter::onError)

            megaApi.removeContact(contact, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    fun removeInShares(user: MegaUser): Completable =
        Completable.fromCallable {
            megaApi.getInShares(user).forEach { node -> megaApi.remove(node) }
        }

    fun hangCurrentCallIfNeeded(userHandle: Long): Completable =
        Completable.create { emitter ->
            if (CallUtil.participatingInACall()) {
                val currentChatId = megaChatApi.getChatRoomByUser(userHandle)?.chatId
                if (currentChatId != null) {
                    megaChatApi.hangChatCall(currentChatId, OptionalMegaChatRequestListenerInterface(
                        onRequestFinish = { _, error ->
                            if (emitter.isDisposed) return@OptionalMegaChatRequestListenerInterface

                            if (error.errorCode == MegaError.API_OK) {
                                emitter.onComplete()
                            } else {
                                emitter.onError(error.toThrowable())
                            }
                        }
                    ))
                } else {
                    emitter.onComplete()
                }
            } else {
                emitter.onComplete()
            }
        }
}
