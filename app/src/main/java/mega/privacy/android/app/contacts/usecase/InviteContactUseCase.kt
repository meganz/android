package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.AFFILIATE_TYPE_CONTACT
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaContactRequest.*
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EEXIST
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to invite contacts
 *
 * @property megaApi            MegaApi required to call the SDK
 * @property databaseHandler    DatabaseHandler required to update public handle
 */
class InviteContactUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    /**
     * Contact link result for getContactLink() method.
     *
     * @property isContact          Flag to check wether is contact or not
     * @property email              User email
     * @property contactHandle      Contact handle
     * @property contactLinkHandle  Contact link handle
     * @property fullName           User full name
     */
    data class ContactLinkResult(
        val isContact: Boolean,
        val email: String? = null,
        val contactHandle: Long? = null,
        val contactLinkHandle: Long? = null,
        val fullName: String? = null
    )

    /**
     * Invite result for invite() method containing all possible states.
     */
    enum class InviteResult {
        SENT, RESENT, DELETED, ALREADY_SENT, ALREADY_CONTACT, INVALID_EMAIL, UNKNOWN_ERROR
    }

    /**
     * Get information about a contact link
     *
     * @param userHandle    Handle of the contact link to check
     * @return              ContactLinkResult
     */
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
                                    request.parentHandle,
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
                    Timber.e(error.toThrowable())
                }
            ))
        }

    /**
     * Invite another person to be your MEGA contact using a contact link handle
     *
     * @param contactLinkHandle Contact link handle of the other account
     * @param email             Email of the new contact
     * @param message           Message for the user
     * @return                  InviteResult
     */
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
                        Timber.e(error.toThrowable())
                    }
                ))
        }

    /**
     * Checks if a contact request is already sent.
     *
     * @param email Contact email
     * @return True if the request is already sent, false otherwise.
     */
    fun isContactRequestAlreadySent(email: String): Single<Boolean> =
        Single.create { emitter ->
            val sentRequests: List<MegaContactRequest> = megaApi.outgoingContactRequests

            for (request in sentRequests) {
                if (request.targetEmail == email) {
                    emitter.onSuccess(true)
                    return@create
                }
            }

            emitter.onSuccess(false)
        }
}
