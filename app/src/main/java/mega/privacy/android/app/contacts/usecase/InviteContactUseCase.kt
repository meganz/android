package mega.privacy.android.app.contacts.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaContactRequest
import javax.inject.Inject

/**
 * Use case to invite contacts
 *
 * @property megaApi            MegaApi required to call the SDK
 */
class InviteContactUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {
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
