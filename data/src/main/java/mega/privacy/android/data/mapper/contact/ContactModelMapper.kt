package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.ContactEntity
import mega.privacy.android.domain.entity.Contact
import javax.inject.Inject

internal class ContactModelMapper @Inject constructor(
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(entity: ContactEntity) = Contact(
        userId = decryptData(entity.handle)?.toLongOrNull() ?: 0L,
        firstName = decryptData(entity.firstName),
        lastName = decryptData(entity.lastName),
        nickname = decryptData(entity.nickName),
        email = decryptData(entity.mail),
    )
}