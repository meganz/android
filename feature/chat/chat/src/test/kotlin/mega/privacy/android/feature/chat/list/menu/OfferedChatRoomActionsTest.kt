package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import org.junit.jupiter.api.Test

class OfferedChatRoomActionsTest {

    private val rooms = listOf(
        ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Chat"),
        ChatRoomItem.IndividualChatRoomItem(chatId = 2L, title = "Chat 2"),
    )

    private val underTest = OfferedChatRoomActions(
        chatIds = setOf(1L, 2L),
        rooms = rooms,
        actionIds = setOf(CHAT_ROOM_ACTIONS_ARCHIVE_TAG),
    )

    @Test
    fun `test that roomsFor returns the offered rooms when the selection and action match`() {
        assertThat(underTest.roomsFor(setOf(1L, 2L), CHAT_ROOM_ACTIONS_ARCHIVE_TAG))
            .isEqualTo(rooms)
    }

    @Test
    fun `test that roomsFor returns null when the selection differs`() {
        assertThat(underTest.roomsFor(setOf(1L), CHAT_ROOM_ACTIONS_ARCHIVE_TAG)).isNull()
    }

    @Test
    fun `test that roomsFor returns null when the action was not offered`() {
        assertThat(underTest.roomsFor(setOf(1L, 2L), CHAT_ROOM_ACTIONS_MUTE_TAG)).isNull()
    }
}
