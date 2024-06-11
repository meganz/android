package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import nz.mega.sdk.MegaChatMessage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [LastMessageTypeMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LastMessageTypeMapperTest {
    private lateinit var underTest: LastMessageTypeMapper

    @BeforeAll
    fun setUp() {
        underTest = LastMessageTypeMapper()
    }

    @TestFactory
    fun `test that the mapping is correct`() = listOf(
        MegaChatMessage.TYPE_INVALID to ChatRoomLastMessage.Invalid,
        MegaChatMessage.TYPE_NORMAL to ChatRoomLastMessage.Normal,
        MegaChatMessage.TYPE_ALTER_PARTICIPANTS to ChatRoomLastMessage.AlterParticipants,
        MegaChatMessage.TYPE_TRUNCATE to ChatRoomLastMessage.Truncate,
        MegaChatMessage.TYPE_PRIV_CHANGE to ChatRoomLastMessage.PrivChange,
        MegaChatMessage.TYPE_CHAT_TITLE to ChatRoomLastMessage.ChatTitle,
        MegaChatMessage.TYPE_CALL_ENDED to ChatRoomLastMessage.CallEnded,
        MegaChatMessage.TYPE_CALL_STARTED to ChatRoomLastMessage.CallStarted,
        MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE to ChatRoomLastMessage.PublicHandleCreate,
        MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE to ChatRoomLastMessage.PublicHandleDelete,
        MegaChatMessage.TYPE_SET_PRIVATE_MODE to ChatRoomLastMessage.SetPrivateMode,
        MegaChatMessage.TYPE_SET_RETENTION_TIME to ChatRoomLastMessage.SetRetentionTime,
        MegaChatMessage.TYPE_SCHED_MEETING to ChatRoomLastMessage.SchedMeeting,
        MegaChatMessage.TYPE_NODE_ATTACHMENT to ChatRoomLastMessage.NodeAttachment,
        MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT to ChatRoomLastMessage.RevokeNodeAttachment,
        MegaChatMessage.TYPE_CONTACT_ATTACHMENT to ChatRoomLastMessage.ContactAttachment,
        MegaChatMessage.TYPE_CONTAINS_META to ChatRoomLastMessage.ContainsMeta,
        MegaChatMessage.TYPE_VOICE_CLIP to ChatRoomLastMessage.VoiceClip,
        MegaChatMessage.TYPE_UNKNOWN to ChatRoomLastMessage.Unknown,
    ).map { (input, expected) ->
        dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(expected)
        }
    }
}