package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.user.UserVisibility
import javax.inject.Inject

/**
 * Mapper to convert data to [ContactData]
 */
internal class ContactDataMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param fullName nullable [String]
     * @param alias nullable [String]
     * @param avatarUri nullable [String]
     *
     * @return [ContactData]
     */
    operator fun invoke(
        fullName: String?,
        alias: String?,
        avatarUri: String?,
        userVisibility: UserVisibility?,
    ) = ContactData(
        fullName = fullName,
        alias = alias,
        avatarUri = avatarUri,
        userVisibility = userVisibility ?: UserVisibility.Unknown
    )
}