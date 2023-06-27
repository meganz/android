package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatGeolocation
import nz.mega.sdk.MegaChatGeolocation
import javax.inject.Inject

/**
 * Mapper for converting [MegaChatGeolocation] into [ChatGeolocation].
 */
internal class ChatGeolocationMapper @Inject constructor() {

    operator fun invoke(megaChatGeolocation: MegaChatGeolocation) = ChatGeolocation(
        longitude = megaChatGeolocation.longitude,
        latitude = megaChatGeolocation.latitude,
        image = megaChatGeolocation.image
    )
}