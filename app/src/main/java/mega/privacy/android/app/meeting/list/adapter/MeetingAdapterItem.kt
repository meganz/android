package mega.privacy.android.app.meeting.list.adapter

import mega.privacy.android.domain.entity.chat.MeetingRoomItem

/**
 * Meeting adapter item
 *
 * @property id     Unique identifier
 */
sealed class MeetingAdapterItem(val id: Long) {

    /**
     * Item representing a Header
     *
     * @property title  Header title
     */
    data class Header constructor(val title: String) : MeetingAdapterItem(title.hashCode().toLong())

    /**
     * Item representing a chat room
     *
     * @property room   Chat room
     */
    data class Data constructor(val room: MeetingRoomItem) : MeetingAdapterItem(room.chatId)
}
