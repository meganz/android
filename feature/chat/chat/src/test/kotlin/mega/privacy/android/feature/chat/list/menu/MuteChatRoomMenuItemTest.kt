package mega.privacy.android.feature.chat.list.menu

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MuteChatRoomMenuItemTest {

    private val muteChatNotificationForChatRoomsUseCase =
        mock<MuteChatNotificationForChatRoomsUseCase>()
    private val unmuteChatNotificationUseCase = mock<UnmuteChatNotificationUseCase>()

    private lateinit var underTest: MuteChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        underTest = MuteChatRoomMenuItem(
            muteChatNotificationForChatRoomsUseCase,
            unmuteChatNotificationUseCase,
        )
    }

    @Test
    fun `test that label is mute for an unmuted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = false)

        assertThat(underTest.label(item)).isEqualTo(sharedR.string.general_mute)
    }

    @Test
    fun `test that label is unmute for a muted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = true)

        assertThat(underTest.label(item)).isEqualTo(sharedR.string.general_unmute)
    }

    @Test
    fun `test that icon is bell off for an unmuted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = false)

        assertThat(underTest.icon(item)).isEqualTo(IconPack.Medium.Thin.Outline.BellOff)
    }

    @Test
    fun `test that icon is bell for a muted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = true)

        assertThat(underTest.icon(item)).isEqualTo(IconPack.Medium.Thin.Outline.Bell)
    }

    @Test
    fun `test that shouldDisplay is true for an active unmuted group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            hasPermissions = true,
            isActive = true,
            isMuted = false,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is true for a muted group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            hasPermissions = true,
            isActive = true,
            isMuted = true,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for an inactive group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            hasPermissions = true,
            isActive = false,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is true for an individual chat with permissions`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            hasPermissions = true,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for an individual chat without permissions`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(
            chatId = 1L,
            title = "Contact",
            hasPermissions = false,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is true for a meeting with permissions`() = runTest {
        val item = ChatRoomItem.MeetingChatRoomItem(
            chatId = 1L,
            title = "Meeting",
            hasPermissions = true,
            isActive = true,
        )

        assertThat(underTest.shouldDisplay(item)).isTrue()
    }

    @Test
    fun `test that shouldDisplay is false for a note to self chat`() = runTest {
        val item = ChatRoomItem.NoteToSelfChatRoomItem(
            chatId = 1L,
            title = "Note to self",
            hasPermissions = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that shouldDisplay is false for an archived group`() = runTest {
        val item = ChatRoomItem.GroupChatRoomItem(
            chatId = 1L,
            title = "Group",
            hasPermissions = true,
            isActive = true,
            isArchived = true,
        )

        assertThat(underTest.shouldDisplay(item)).isFalse()
    }

    @Test
    fun `test that onClick mutes an unmuted chat until turned back on`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 7L, title = "Contact", isMuted = false)

        underTest.onClick(item)

        verify(muteChatNotificationForChatRoomsUseCase)(
            chatIdList = listOf(7L),
            muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
        )
    }

    @Test
    fun `test that onClick unmutes a muted chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 7L, title = "Contact", isMuted = true)

        underTest.onClick(item)

        verify(unmuteChatNotificationUseCase)(7L)
    }
}
