package mega.privacy.android.domain.usecase.contact.group

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactGroupsUseCaseTest {

    private lateinit var underTest: GetContactGroupsUseCase

    private val chatRepository = mock<ChatRepository>()
    private val getContactGroupAvatarsUseCase = mock<GetContactGroupAvatarsUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = GetContactGroupsUseCase(
            chatRepository = chatRepository,
            getContactGroupAvatarsUseCase = getContactGroupAvatarsUseCase,
        )
        getContactGroupAvatarsUseCase.stub {
            on { invoke(any()) } doReturn emptyMap()
        }
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository, getContactGroupAvatarsUseCase)
    }

    @Test
    fun `test that non group rooms are filtered out`() = runTest {
        whenever(chatRepository.getChatRooms()).thenReturn(
            listOf(chatRoom(chatId = 1L, title = "Alpha", isGroup = false))
        )

        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that group rooms without peers are filtered out`() = runTest {
        whenever(chatRepository.getChatRooms()).thenReturn(
            listOf(chatRoom(chatId = 1L, title = "Alpha", peerCount = 0L))
        )

        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that groups are sorted case insensitively by title`() = runTest {
        whenever(chatRepository.getChatRooms()).thenReturn(
            listOf(
                chatRoom(chatId = 1L, title = "beta"),
                chatRoom(chatId = 2L, title = "Alpha"),
                chatRoom(chatId = 3L, title = "Charlie"),
            )
        )

        assertThat(underTest().map { it.title })
            .containsExactly("Alpha", "beta", "Charlie")
            .inOrder()
    }

    @Test
    fun `test that avatars are mapped onto the matching chat id`() = runTest {
        val avatars = listOf(ChatAvatarItem(placeholderText = "Alpha"))
        whenever(chatRepository.getChatRooms()).thenReturn(
            listOf(chatRoom(chatId = 1L, title = "Alpha"))
        )
        getContactGroupAvatarsUseCase.stub {
            on { invoke(any()) } doReturn mapOf(1L to avatars)
        }

        assertThat(underTest().single().avatar).isEqualTo(avatars)
    }

    private fun chatRoom(
        chatId: Long,
        title: String,
        isGroup: Boolean = true,
        peerCount: Long = 2L,
    ) = ChatRoom(
        chatId = chatId,
        ownPrivilege = ChatRoomPermission.Standard,
        numPreviewers = 0L,
        peerPrivilegesByHandles = emptyMap(),
        peerCount = peerCount,
        peerHandlesList = emptyList(),
        peerPrivilegesList = emptyList(),
        isGroup = isGroup,
        isPublic = true,
        isPreview = false,
        authorizationToken = null,
        title = title,
        hasCustomTitle = false,
        unreadCount = 0,
        userTyping = 0L,
        userHandle = 0L,
        isActive = true,
        isArchived = false,
        retentionTime = 0L,
        creationTime = 0L,
        isMeeting = false,
        isWaitingRoom = false,
        isOpenInvite = false,
        isSpeakRequest = false,
        isNoteToSelf = false,
    )
}
