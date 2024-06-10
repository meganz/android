package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import nz.mega.sdk.MegaChatListItem
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [ChatListItemChangesMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatListItemChangesMapperTest {
    private lateinit var underTest: ChatListItemChangesMapper

    @BeforeAll
    fun setUp() {
        underTest = ChatListItemChangesMapper()
    }

    @TestFactory
    fun `test that the mapping is correct`() = listOf(
        MegaChatListItem.CHANGE_TYPE_STATUS to ChatListItemChanges.Status,
        MegaChatListItem.CHANGE_TYPE_OWN_PRIV to ChatListItemChanges.OwnPrivilege,
        MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT to ChatListItemChanges.UnreadCount,
        MegaChatListItem.CHANGE_TYPE_PARTICIPANTS to ChatListItemChanges.Participants,
        MegaChatListItem.CHANGE_TYPE_TITLE to ChatListItemChanges.Title,
        MegaChatListItem.CHANGE_TYPE_CLOSED to ChatListItemChanges.Closed,
        MegaChatListItem.CHANGE_TYPE_LAST_MSG to ChatListItemChanges.LastMessage,
        MegaChatListItem.CHANGE_TYPE_LAST_TS to ChatListItemChanges.LastTS,
        MegaChatListItem.CHANGE_TYPE_ARCHIVE to ChatListItemChanges.Archive,
        MegaChatListItem.CHANGE_TYPE_CALL to ChatListItemChanges.Call,
        MegaChatListItem.CHANGE_TYPE_CHAT_MODE to ChatListItemChanges.ChatMode,
        MegaChatListItem.CHANGE_TYPE_UPDATE_PREVIEWERS to ChatListItemChanges.UpdatePreviewers,
        MegaChatListItem.CHANGE_TYPE_PREVIEW_CLOSED to ChatListItemChanges.PreviewClosed,
        MegaChatListItem.CHANGE_TYPE_DELETED to ChatListItemChanges.Deleted,
    ).map { (input, expected) ->
        dynamicTest("test that $input is mapped to $expected") {
            assertThat(underTest(input)).isEqualTo(expected)
        }
    }

    @Test
    fun `test that an unspecified value is mapped to an unknown chat list item changes enum`() =
        runTest {
            // 200 is unspecified in the Mapper
            assertThat(underTest(200)).isEqualTo(ChatListItemChanges.Unknown)
        }
}