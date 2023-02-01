package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import nz.mega.sdk.MegaError

/**
 * Map [MegaError] to [InviteContactRequest]
 */
typealias InviteContactRequestMapper = (@JvmSuppressWildcards MegaError) -> @JvmSuppressWildcards InviteContactRequest

internal fun toInviteContactRequest(error: MegaError): InviteContactRequest {
    return when (error.errorCode) {
        MegaError.API_OK -> InviteContactRequest.Sent
        MegaError.API_EEXIST -> InviteContactRequest.AlreadyContact
        MegaError.API_EARGS -> InviteContactRequest.InvalidEmail
        else -> InviteContactRequest.InvalidStatus
    }
}