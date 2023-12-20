package mega.privacy.android.domain.entity.chat.messages.meta

import mega.privacy.android.domain.entity.chat.ChatGeolocation

/**
 * Location message
 *
 * @property geolocation [ChatGeolocation]
 */
data class LocationMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    val geolocation: ChatGeolocation?,
) : MetaMessage