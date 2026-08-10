package mega.privacy.android.shared.contact.mapper

import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import javax.inject.Inject

/**
 * Contact item status mapper
 *
 */
class ContactItemStatusMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param status
     * @return mapped status
     */
    operator fun invoke(status: UserChatStatus): ContactItemStatus = when (status) {
        UserChatStatus.Online -> ContactItemStatus.Online
        UserChatStatus.Away -> ContactItemStatus.Away
        UserChatStatus.Busy -> ContactItemStatus.Busy
        UserChatStatus.Offline -> ContactItemStatus.Offline
        UserChatStatus.Invalid -> ContactItemStatus.Unknown
    }
}