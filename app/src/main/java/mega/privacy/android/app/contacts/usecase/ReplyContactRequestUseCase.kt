package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaContactRequest.*
import nz.mega.sdk.MegaError
import javax.inject.Inject

class ReplyContactRequestUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    fun acceptReceivedRequest(requestHandle: Long): Completable =
        replyToReceivedRequest(requestHandle, REPLY_ACTION_ACCEPT)

    fun ignoreReceivedRequest(requestHandle: Long): Completable =
        replyToReceivedRequest(requestHandle, REPLY_ACTION_ACCEPT)

    fun denyReceivedRequest(requestHandle: Long): Completable =
        replyToReceivedRequest(requestHandle, REPLY_ACTION_DENY)

    fun remindSentRequest(requestHandle: Long): Completable =
        replyToSentRequest(requestHandle, INVITE_ACTION_REMIND)

    fun deleteSentRequest(requestHandle: Long): Completable =
        replyToSentRequest(requestHandle, INVITE_ACTION_DELETE)

    private fun replyToReceivedRequest(requestHandle: Long, replyAction: Int): Completable =
        Completable.create { emitter ->
            val contactRequest = megaApi.getContactRequestByHandle(requestHandle)

            megaApi.replyContactRequest(
                contactRequest,
                replyAction,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                        if (error.errorCode == MegaError.API_OK) {
                            emitter.onComplete()
                        } else {
                            emitter.onError(error.toThrowable())
                        }
                    },
                    onRequestTemporaryError = { _, error ->
                        LogUtil.logError(error.toThrowable().stackTraceToString())
                    }
                ))
        }

    private fun replyToSentRequest(requestHandle: Long, replyAction: Int): Completable =
        Completable.create { emitter ->
            val contactRequest = megaApi.getContactRequestByHandle(requestHandle)

            megaApi.inviteContact(
                contactRequest.targetEmail,
                null,
                replyAction,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                        if (error.errorCode == MegaError.API_OK) {
                            emitter.onComplete()
                        } else {
                            emitter.onError(error.toThrowable())
                        }
                    },
                    onRequestTemporaryError = { _, error ->
                        LogUtil.logError(error.toThrowable().stackTraceToString())
                    }
                ))
        }
}
