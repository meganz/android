package mega.privacy.android.feature.contact.group.mapper

import mega.privacy.android.domain.entity.contacts.group.ContactGroup
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.shared.contact.mapper.ChatAvatarItemMapper
import javax.inject.Inject

/**
 * Contact group item mapper
 *
 * @property avatarItemMapper
 */
class ContactGroupItemMapper @Inject constructor(
    private val avatarItemMapper: ChatAvatarItemMapper,
) {

    /**
     * Invoke
     *
     * @param group
     */
    operator fun invoke(group: ContactGroup) = ContactGroupItem(
        chatId = group.chatId,
        name = group.title,
        avatarData = group.avatar.map { avatarItemMapper(it) },
        isPrivate = group.isPublic.not()
    )
}