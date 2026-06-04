package mega.privacy.android.feature.contact.group.model

import mega.privacy.android.shared.contact.model.AvatarData

/**
 * Contact group item
 *
 * @property chatId
 * @property name
 * @property avatarData
 * @property isPrivate
 */
data class ContactGroupItem(
    val chatId: Long,
    val name: String,
    val avatarData: List<AvatarData>,
    val isPrivate: Boolean,
)
