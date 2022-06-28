package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaContactRequest.*
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to reply to existing contact requests for current user.
 *
 * @property megaApi    MegaApi required to call the SDK
 */
class ReplyContactRequestUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    fun acceptReceivedRequest(requestHandle: Long): Completable =
        handleReceivedRequest(requestHandle, REPLY_ACTION_ACCEPT)

    fun ignoreReceivedRequest(requestHandle: Long): Completable =
        handleReceivedRequest(requestHandle, REPLY_ACTION_IGNORE)

    fun denyReceivedRequest(requestHandle: Long): Completable =
        handleReceivedRequest(requestHandle, REPLY_ACTION_DENY)

    fun remindSentRequest(requestHandle: Long): Completable =
        handleSentRequest(requestHandle, INVITE_ACTION_REMIND)

    fun deleteSentRequest(requestHandle: Long): Completable =
        handleSentRequest(requestHandle, INVITE_ACTION_DELETE)

    /**
     * Reply with an action to a previously previously received Contact Request
     *
     * @param requestHandle     Contact request Id
     * @param action            Action for this contact request
     * @return                  Completable
     */
    private fun handleReceivedRequest(requestHandle: Long, action: Int): Completable =
        Completable.create { emitter ->
            val contactRequest = megaApi.getContactRequestByHandle(requestHandle)

            if (contactRequest != null && !contactRequest.isOutgoing) {
                megaApi.replyContactRequest(
                    contactRequest,
                    action,
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
                            Timber.e(error.toThrowable())
                        }
                    ))
            } else {
                emitter.onError(IllegalArgumentException("Not a received contact request"))
            }
        }

    /**
     * Send an action to a previously sent Contact Request
     *
     * @param requestHandle     Contact request Id
     * @param action            Action for this contact request
     * @return                  Completable
     */
    private fun handleSentRequest(requestHandle: Long, action: Int): Completable =
        Completable.create { emitter ->
            val contactRequest = megaApi.getContactRequestByHandle(requestHandle)

            if (contactRequest != null && contactRequest.isOutgoing) {
                megaApi.inviteContact(
                    contactRequest.targetEmail,
                    contactRequest.sourceMessage,
                    action,
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
                            Timber.e(error.toThrowable())
                        }
                    ))
            } else {
                emitter.onError(IllegalArgumentException("Not a sent contact request"))
            }
        }
}
