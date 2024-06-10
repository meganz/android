package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import nz.mega.sdk.MegaChatRoom
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ChatRoomChangesMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatRoomChangesMapperTest {
    private lateinit var underTest: ChatRoomChangesMapper

    @BeforeAll
    fun setUp() {
        underTest = ChatRoomChangesMapper()
    }

    @TestFactory
    fun `test that the mapping is correct`() = listOf(
        MegaChatRoom.CHANGE_TYPE_STATUS to ChatRoomChange.Status,
        MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT to ChatRoomChange.UnreadCount,
        MegaChatRoom.CHANGE_TYPE_PARTICIPANTS to ChatRoomChange.Participants,
        MegaChatRoom.CHANGE_TYPE_TITLE to ChatRoomChange.Title,
        MegaChatRoom.CHANGE_TYPE_USER_TYPING to ChatRoomChange.UserTyping,
        MegaChatRoom.CHANGE_TYPE_CLOSED to ChatRoomChange.Closed,
        MegaChatRoom.CHANGE_TYPE_OWN_PRIV to ChatRoomChange.OwnPrivilege,
        MegaChatRoom.CHANGE_TYPE_USER_STOP_TYPING to ChatRoomChange.UserStopTyping,
        MegaChatRoom.CHANGE_TYPE_ARCHIVE to ChatRoomChange.Archive,
        MegaChatRoom.CHANGE_TYPE_CHAT_MODE to ChatRoomChange.ChatMode,
        MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS to ChatRoomChange.UpdatePreviewers,
        MegaChatRoom.CHANGE_TYPE_RETENTION_TIME to ChatRoomChange.RetentionTime,
        MegaChatRoom.CHANGE_TYPE_OPEN_INVITE to ChatRoomChange.OpenInvite,
        MegaChatRoom.CHANGE_TYPE_SPEAK_REQUEST to ChatRoomChange.SpeakRequest,
        MegaChatRoom.CHANGE_TYPE_WAITING_ROOM to ChatRoomChange.WaitingRoom,
    ).map { (input, expected) ->
        dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(listOf(expected))
        }
    }
}