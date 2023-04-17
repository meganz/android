package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.ContactEntity
import mega.privacy.android.domain.entity.Contact
import javax.inject.Inject

internal class ContactEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
) {
    operator fun invoke(contact: Contact) = ContactEntity(
        handle = encryptData(contact.userId.toString()),
        firstName = encryptData(contact.firstName),
        lastName = encryptData(contact.lastName),
        mail = encryptData(contact.email),
        nickName = encryptData(contact.nickname),
    )
}