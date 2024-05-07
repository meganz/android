package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import nz.mega.sdk.MegaContactRequest
import javax.inject.Inject

/**
 * Mapper used to map contact request actions
 */
class ContactRequestActionMapper @Inject constructor() {
    /**
     * invoke
     *
     * @param contactRequestAction contact request action
     */
    operator fun invoke(contactRequestAction: ContactRequestAction): Int =
        when (contactRequestAction) {
            ContactRequestAction.Accept -> MegaContactRequest.REPLY_ACTION_ACCEPT
            ContactRequestAction.Deny -> MegaContactRequest.REPLY_ACTION_DENY
            ContactRequestAction.Ignore -> MegaContactRequest.REPLY_ACTION_IGNORE
            ContactRequestAction.Add -> MegaContactRequest.INVITE_ACTION_ADD
            ContactRequestAction.Delete -> MegaContactRequest.INVITE_ACTION_DELETE
            ContactRequestAction.Remind -> MegaContactRequest.INVITE_ACTION_REMIND
        }
}
