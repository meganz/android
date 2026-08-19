package mega.privacy.android.feature.chat.list.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.feature.chat.list.model.ChatRoomUiItem.ChatRoomUiAvatar
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRoomUiItemMapperTest {

    private lateinit var underTest: ChatRoomUiItemMapper

    @BeforeEach
    fun setUp() {
        underTest = ChatRoomUiItemMapper()
    }

    @Test
    fun `test that invoke maps an individual chat room item`() {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Mieko Kawakami",
            lastMessage = "See you tomorrow!",
            lastTimestampFormatted = "Today 14:25",
            unreadCount = 5,
            isMuted = true,
            highlight = true,
            avatar = ChatAvatarItem(
                placeholderText = "M",
                uri = "/avatars/mieko.jpg",
                color = 0xFFFEBC00.toInt(),
            ),
        )

        val actual = underTest(item)

        assertThat(actual.chatId).isEqualTo(1L)
        assertThat(actual.title).isEqualTo("Mieko Kawakami")
        assertThat(actual.lastMessage).isEqualTo("See you tomorrow!")
        assertThat(actual.lastTimestampFormatted).isEqualTo("Today 14:25")
        assertThat(actual.scheduledTimestampFormatted).isNull()
        assertThat(actual.unreadCount).isEqualTo(5)
        assertThat(actual.isMuted).isTrue()
        assertThat(actual.highlight).isTrue()
        assertThat(actual.isNoteToSelf).isFalse()
        assertThat(actual.avatar).isEqualTo(
            ChatRoomUiAvatar.Peer(
                placeholderText = "M",
                filePath = "/avatars/mieko.jpg",
                color = 0xFFFEBC00.toInt(),
            )
        )
    }

    @Test
    fun `test that invoke maps a group chat room item to a group avatar`() {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 2L,
            title = "Recipe test #14",
        )

        val actual = underTest(item)

        assertThat(actual.avatar).isEqualTo(ChatRoomUiAvatar.Group)
    }

    @Test
    fun `test that invoke maps a note to self chat room item`() {
        val item = ChatRoomItem.NoteToSelfChatRoomItem(
            chatId = 3L,
            title = "Note to self",
            isMuted = true,
        )

        val actual = underTest(item)

        assertThat(actual.isNoteToSelf).isTrue()
        assertThat(actual.avatar).isEqualTo(ChatRoomUiAvatar.NoteToSelf)
        assertThat(actual.isMuted).isFalse()
    }

    @Test
    fun `test that invoke maps the scheduled timestamp when the meeting is pending`() {
        val item = ChatRoomItem.MeetingChatRoomItem(
            chatId = 4L,
            title = "Weekly sync",
            isPending = true,
            scheduledTimestampFormatted = "10:00am - 11:00am",
        )

        val actual = underTest(item)

        assertThat(actual.avatar).isEqualTo(ChatRoomUiAvatar.Meeting)
        assertThat(actual.scheduledTimestampFormatted).isEqualTo("10:00am - 11:00am")
    }

    @Test
    fun `test that invoke maps a null scheduled timestamp when the meeting is cancelled`() {
        val item = ChatRoomItem.MeetingChatRoomItem(
            chatId = 4L,
            title = "Weekly sync",
            isPending = true,
            isCancelled = true,
            scheduledTimestampFormatted = "10:00am - 11:00am",
        )

        val actual = underTest(item)

        assertThat(actual.scheduledTimestampFormatted).isNull()
    }

    @Test
    fun `test that invoke maps a blank last message to null`() {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 5L,
            title = "Chat",
            lastMessage = " ",
        )

        val actual = underTest(item)

        assertThat(actual.lastMessage).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideUserChatStatuses")
    fun `test that invoke maps the user chat status of an individual chat room item`(
        userChatStatus: UserChatStatus,
        expected: ContactItemStatus,
    ) {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Mieko Kawakami",
            userChatStatus = userChatStatus,
        )

        val actual = underTest(item)

        assertThat(actual.status).isEqualTo(expected)
    }

    @Test
    fun `test that invoke maps a null user chat status to unknown`() {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Mieko Kawakami",
            userChatStatus = null,
        )

        val actual = underTest(item)

        assertThat(actual.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that invoke maps a group chat room item to an unknown status`() {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 2L,
            title = "Recipe test #14",
        )

        val actual = underTest(item)

        assertThat(actual.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that invoke maps a meeting chat room item to an unknown status`() {
        val item = ChatRoomItem.MeetingChatRoomItem(
            chatId = 4L,
            title = "Weekly sync",
        )

        val actual = underTest(item)

        assertThat(actual.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that invoke maps a note to self chat room item to an unknown status`() {
        val item = ChatRoomItem.NoteToSelfChatRoomItem(
            chatId = 3L,
            title = "Note to self",
            userChatStatus = UserChatStatus.Online,
        )

        val actual = underTest(item)

        assertThat(actual.status).isEqualTo(ContactItemStatus.Unknown)
    }

    private fun provideUserChatStatuses(): Stream<Arguments> = Stream.of(
        Arguments.of(UserChatStatus.Online, ContactItemStatus.Online),
        Arguments.of(UserChatStatus.Away, ContactItemStatus.Away),
        Arguments.of(UserChatStatus.Busy, ContactItemStatus.Busy),
        Arguments.of(UserChatStatus.Offline, ContactItemStatus.Offline),
        Arguments.of(UserChatStatus.Invalid, ContactItemStatus.Unknown),
    )
}
