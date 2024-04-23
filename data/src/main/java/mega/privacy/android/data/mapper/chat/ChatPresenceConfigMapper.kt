package mega.privacy.android.data.mapper.chat

import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import nz.mega.sdk.MegaChatPresenceConfig
import javax.inject.Inject

internal class ChatPresenceConfigMapper @Inject constructor(
    private val userChatStatusMapper: UserChatStatusMapper,
) {

    operator fun invoke(megaConfig: MegaChatPresenceConfig): ChatPresenceConfig =
        ChatPresenceConfig(
            onlineStatus = userChatStatusMapper(megaConfig.onlineStatus),
            isAutoAwayEnabled = megaConfig.isAutoawayEnabled,
            autoAwayTimeout = megaConfig.autoawayTimeout,
            isPersist = megaConfig.isPersist,
            isPending = megaConfig.isPending,
            isLastGreenVisible = megaConfig.isLastGreenVisible
        )
}
