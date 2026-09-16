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
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MuteChatRoomMenuItemTest {

    private val muteChatNotificationForChatRoomsUseCase =
        mock<MuteChatNotificationForChatRoomsUseCase>()
    private val unmuteChatNotificationUseCase = mock<UnmuteChatNotificationUseCase>()

    private lateinit var underTest: MuteChatRoomMenuItem

    @BeforeEach
    fun setUp() {
        reset(muteChatNotificationForChatRoomsUseCase, unmuteChatNotificationUseCase)
        underTest = MuteChatRoomMenuItem(
            muteChatNotificationForChatRoomsUseCase,
            unmuteChatNotificationUseCase,
        )
    }

    @Test
    fun `test that label is mute for an unmuted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = false)

        assertThat(underTest.label(listOf(item))).isEqualTo(sharedR.string.general_mute)
    }

    @Test
    fun `test that label is unmute for a muted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = true)

        assertThat(underTest.label(listOf(item))).isEqualTo(sharedR.string.general_unmute)
    }

    @Test
    fun `test that icon is bell off for an unmuted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = false)

        assertThat(underTest.icon(listOf(item))).isEqualTo(IconPack.Medium.Thin.Outline.BellOff)
    }

    @Test
    fun `test that icon is bell for a muted chat`() {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Contact", isMuted = true)

        assertThat(underTest.icon(listOf(item))).isEqualTo(IconPack.Medium.Thin.Outline.Bell)
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

        underTest.onClick(listOf(item))

        verify(muteChatNotificationForChatRoomsUseCase)(
            chatIdList = listOf(7L),
            muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
        )
    }

    @Test
    fun `test that onClick unmutes a muted chat`() = runTest {
        val item = ChatRoomItem.IndividualChatRoomItem(chatId = 7L, title = "Contact", isMuted = true)

        underTest.onClick(listOf(item))

        verify(unmuteChatNotificationUseCase)(7L)
    }

    @Test
    fun `test that label is mute when the selection mixes muted and unmuted chats`() {
        assertThat(underTest.label(mixedSelection())).isEqualTo(sharedR.string.general_mute)
    }

    @Test
    fun `test that icon is bell off when the selection mixes muted and unmuted chats`() {
        assertThat(underTest.icon(mixedSelection()))
            .isEqualTo(IconPack.Medium.Thin.Outline.BellOff)
    }

    @Test
    fun `test that onClick mutes every chat when the selection mixes muted and unmuted chats`() =
        runTest {
            underTest.onClick(mixedSelection())

            verify(muteChatNotificationForChatRoomsUseCase)(
                chatIdList = listOf(1L, 2L),
                muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
            )
            verifyNoInteractions(unmuteChatNotificationUseCase)
        }

    @Test
    fun `test that label is unmute when every chat in the selection is muted`() {
        val items = listOf(
            ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Muted", isMuted = true),
            ChatRoomItem.IndividualChatRoomItem(chatId = 2L, title = "Also muted", isMuted = true),
        )

        assertThat(underTest.label(items)).isEqualTo(sharedR.string.general_unmute)
    }

    @Test
    fun `test that onClick unmutes every chat when every chat in the selection is muted`() =
        runTest {
            val items = listOf(
                ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Muted", isMuted = true),
                ChatRoomItem.IndividualChatRoomItem(
                    chatId = 2L,
                    title = "Also muted",
                    isMuted = true,
                ),
            )

            underTest.onClick(items)

            verify(unmuteChatNotificationUseCase)(1L)
            verify(unmuteChatNotificationUseCase)(2L)
            verifyNoInteractions(muteChatNotificationForChatRoomsUseCase)
        }

    private fun mixedSelection() = listOf(
        ChatRoomItem.IndividualChatRoomItem(chatId = 1L, title = "Unmuted", isMuted = false),
        ChatRoomItem.IndividualChatRoomItem(chatId = 2L, title = "Muted", isMuted = true),
    )
}
