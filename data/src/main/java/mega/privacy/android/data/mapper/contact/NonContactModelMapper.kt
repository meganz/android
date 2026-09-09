package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.database.entity.NonContactEntity
import mega.privacy.android.data.model.chat.NonContactInfo
import javax.inject.Inject

internal class NonContactModelMapper @Inject constructor() {
    operator fun invoke(entity: NonContactEntity) = NonContactInfo(
        handle = entity.handle.toString(),
        fullName = entity.fullName,
        firstName = entity.firstName,
        lastName = entity.lastName,
        email = entity.email,
    )
}
