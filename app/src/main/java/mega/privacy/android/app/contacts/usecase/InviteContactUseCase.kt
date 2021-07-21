package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.AFFILIATE_TYPE_CONTACT
import nz.mega.sdk.MegaContactRequest.*
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaUser
import javax.inject.Inject

class InviteContactUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    data class ContactLinkResult(
        val isContact: Boolean,
        val email: String? = null,
        val contactLinkHandle: Long? = null,
        val fullName: String? = null
    )

    enum class InviteResult {
        SENT, RESENT, DELETED, ALREADY_SENT, ALREADY_CONTACT, INVALID_EMAIL, UNKNOWN_ERROR
    }

    fun getContactLink(userHandle: Long): Single<ContactLinkResult> =
        Single.create { emitter ->
            megaApi.contactLinkQuery(userHandle, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    when (error.errorCode) {
                        API_OK -> {
                            databaseHandler.setLastPublicHandle(request.nodeHandle)
                            databaseHandler.setLastPublicHandleTimeStamp()
                            databaseHandler.lastPublicHandleType = AFFILIATE_TYPE_CONTACT

                            val isContact = megaApi.contacts.any { contact ->
                                contact.email == request.email
                                        && contact.visibility == MegaUser.VISIBILITY_VISIBLE
                            }

                            emitter.onSuccess(
                                ContactLinkResult(
                                    isContact,
                                    request.email,
                                    request.nodeHandle,
                                    "${request.name} ${request.text}"
                                )
                            )
                        }
                        API_EEXIST -> {
                            emitter.onSuccess(ContactLinkResult(false))
                        }
                        else -> {
                            emitter.onError(error.toThrowable())
                        }
                    }
                },
                onRequestTemporaryError = { _, error ->
                    logError(error.toThrowable().stackTraceToString())
                }
            ))
        }

    @JvmOverloads
    fun invite(
        contactLinkHandle: Long,
        email: String,
        message: String? = null
    ): Single<InviteResult> =
        Single.create { emitter ->
            megaApi.inviteContact(
                email,
                message,
                INVITE_ACTION_ADD,
                contactLinkHandle,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                        when {
                            request.number == INVITE_ACTION_REMIND.toLong() -> {
                                emitter.onSuccess(InviteResult.RESENT)
                            }
                            error.errorCode == API_OK && request.number == INVITE_ACTION_ADD.toLong() -> {
                                emitter.onSuccess(InviteResult.SENT)
                            }
                            error.errorCode == API_OK && request.number == INVITE_ACTION_DELETE.toLong() -> {
                                emitter.onSuccess(InviteResult.DELETED)
                            }
                            error.errorCode == API_EEXIST -> {
                                if (megaApi.outgoingContactRequests.any { it.targetEmail == request.email }) {
                                    emitter.onSuccess(InviteResult.ALREADY_SENT)
                                } else {
                                    emitter.onSuccess(InviteResult.ALREADY_CONTACT)
                                }
                            }
                            request.number == INVITE_ACTION_ADD.toLong() && error.errorCode == MegaError.API_EARGS -> {
                                emitter.onSuccess(InviteResult.INVALID_EMAIL)
                            }
                            else -> {
                                emitter.onError(error.toThrowable())
                            }
                        }
                    },
                    onRequestTemporaryError = { _, error ->
                        logError(error.toThrowable().stackTraceToString())
                    }
                ))
        }
}
