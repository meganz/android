package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Map [MegaError] to [InviteContactRequest]
 */
internal class InviteContactRequestMapper @Inject constructor() {

    operator fun invoke(error: MegaError): InviteContactRequest {
        return when (error.errorCode) {
            MegaError.API_OK -> InviteContactRequest.Sent
            MegaError.API_EEXIST -> InviteContactRequest.AlreadyContact
            MegaError.API_EARGS -> InviteContactRequest.InvalidEmail
            else -> InviteContactRequest.InvalidStatus
        }
    }
}