package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.ContactRequest
import mega.privacy.android.domain.entity.ContactRequestStatus
import nz.mega.sdk.MegaContactRequest

/**
 * Map [MegaContactRequest] to [ContactRequest]
 */
typealias ContactRequestMapper = (@JvmSuppressWildcards MegaContactRequest) -> ContactRequest

/**
 * Map [MegaContactRequest] to [ContactRequest]
 */
internal fun toContactRequest(megaRequest: MegaContactRequest) = ContactRequest(
    handle = megaRequest.handle,
    sourceEmail = megaRequest.sourceEmail,
    sourceMessage = megaRequest.sourceMessage,
    targetEmail = megaRequest.targetEmail,
    creationTime = megaRequest.creationTime,
    modificationTime = megaRequest.modificationTime,
    status = mapStatus(megaRequest.status),
    isOutgoing = megaRequest.isOutgoing,
    isAutoAccepted = megaRequest.isAutoAccepted,
)

private fun mapStatus(status: Int): ContactRequestStatus = when (status) {
    MegaContactRequest.STATUS_ACCEPTED -> ContactRequestStatus.Accepted
    MegaContactRequest.STATUS_DENIED -> ContactRequestStatus.Denied
    MegaContactRequest.STATUS_IGNORED -> ContactRequestStatus.Ignored
    MegaContactRequest.STATUS_DELETED -> ContactRequestStatus.Deleted
    MegaContactRequest.STATUS_REMINDED -> ContactRequestStatus.Reminded
    else -> ContactRequestStatus.Unresolved
}



