package mega.privacy.android.data.mapper

import mega.privacy.android.data.extensions.toException
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Map [MegaError] to [InviteContactRequest]
 */
internal class InviteContactRequestMapper @Inject constructor() {

    @Throws(MegaException::class)
    suspend operator fun invoke(
        error: MegaError,
        email: String,
        getOutgoingContactRequests: suspend () -> List<MegaContactRequest>,
        getIncomingContactRequests: suspend () -> List<MegaContactRequest>,
    ): InviteContactRequest {
        return when (error.errorCode) {
            MegaError.API_OK -> InviteContactRequest.Sent
            MegaError.API_EEXIST -> {
                when {
                    getOutgoingContactRequests().any { it.targetEmail == email } -> {
                        InviteContactRequest.AlreadySent
                    }

                    getIncomingContactRequests().any { it.sourceEmail == email } -> {
                        InviteContactRequest.AlreadyReceived
                    }

                    else -> {
                        InviteContactRequest.AlreadyContact
                    }
                }
            }

            MegaError.API_EARGS -> InviteContactRequest.InvalidEmail
            else -> throw error.toException("inviteContact")
        }
    }
}