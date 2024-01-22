package mega.privacy.android.domain.entity.chat.messages.meta

import mega.privacy.android.domain.entity.chat.ChatGeolocation
import mega.privacy.android.domain.entity.chat.messages.ChatGeolocationInfo

/**
 * Location message
 *
 * @property chatGeolocationInfo [ChatGeolocation]
 */
data class LocationMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    val chatGeolocationInfo: ChatGeolocationInfo?,
) : MetaMessage