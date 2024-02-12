package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatGeolocation
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Location message
 *
 * @property chatGeolocationInfo [ChatGeolocation]
 */
@Serializable
data class LocationMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val chatGeolocationInfo: ChatGeolocationInfo?,
) : MetaMessage