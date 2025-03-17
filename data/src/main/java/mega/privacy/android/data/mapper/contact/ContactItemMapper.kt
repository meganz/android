package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Mapper for converting [MegaUser] into [ContactItem]
 */
internal class ContactItemMapper @Inject constructor(
    private val userChatStatusMapper: UserChatStatusMapper,
    private val userVisibilityMapper: UserVisibilityMapper
) {
    /**
     * Invoke
     *
     * @param megaUser [MegaUser]
     * @param contactData [ContactData]
     * @param defaultAvatarColor [String]
     * @param areCredentialsVerified [Boolean]
     * @param status [Int]
     * @param lastSeen nullable [Int]
     *
     * @return the [ContactItem] of [megaUser]
     */
    operator fun invoke(
        megaUser: MegaUser,
        contactData: ContactData,
        defaultAvatarColor: String?,
        areCredentialsVerified: Boolean,
        status: Int,
        lastSeen: Int?,
        chatRoomId: Long?,
    ) = ContactItem(
        handle = megaUser.handle,
        email = megaUser.email,
        contactData = contactData,
        defaultAvatarColor = defaultAvatarColor,
        visibility = userVisibilityMapper(megaUser),
        timestamp = megaUser.timestamp,
        areCredentialsVerified = areCredentialsVerified,
        status = userChatStatusMapper(status),
        lastSeen = lastSeen,
        chatroomId = chatRoomId,
    )
}