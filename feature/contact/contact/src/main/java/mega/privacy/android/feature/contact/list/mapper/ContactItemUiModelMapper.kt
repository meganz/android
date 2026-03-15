package mega.privacy.android.feature.contact.list.mapper

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.feature.contact.list.model.ContactUiModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Maps domain [ContactItem] to [ContactUiModel].
 */
internal class ContactItemUiModelMapper @Inject constructor() {

    /**
     * @param contactItem Domain contact item.
     * @return UI model for the contact.
     */
    operator fun invoke(contactItem: ContactItem): ContactUiModel {
        val alias = contactItem.contactData.alias
        val fullName = contactItem.contactData.fullName
        val displayName = when {
            !alias.isNullOrBlank() -> alias
            !fullName.isNullOrBlank() -> fullName
            else -> contactItem.email
        }
        return ContactUiModel(
            handle = contactItem.handle,
            email = contactItem.email,
            displayName = displayName,
            fullName = fullName,
            alias = alias,
            status = contactItem.status,
            avatarUri = contactItem.contactData.avatarUri,
            avatarColor = contactItem.defaultAvatarColor,
            lastSeenSeconds = contactItem.lastSeen,
            isNew = isWithinLastThreeDays(contactItem.timestamp) && contactItem.chatroomId == null,
            isVerified = contactItem.areCredentialsVerified,
        )
    }

    private fun isWithinLastThreeDays(timestamp: Long): Boolean {
        val now = LocalDateTime.now()
        val addedTime = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return Duration.between(addedTime, now).toDays() < 3
    }
}
