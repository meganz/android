package mega.privacy.android.app.usecase

import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.app.contacts.usecase.GetContactGroupsUseCase
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import mega.privacy.android.app.presentation.meeting.model.newChatRoom
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactGroupsUseCaseTest {

    private lateinit var underTest: GetContactGroupsUseCase

    private val chatRepository = mock<ChatRepository>()
    private val avatarRepository = mock<AvatarRepository>()
    private val contactsRepository = mock<ContactsRepository>()

    private val firstHandle = 666L
    private val lastHandle = 777L

    private val noGroupRoom = listOf(
        newChatRoom(
            withChatId = 1L,
            withPeerCount = 0L,
            withPeerHandlesList = listOf(firstHandle, lastHandle),
            withPeerPrivilegesList = emptyList(),
            withIsGroup = false,
        )
    )

    private val noPeersRoom = listOf(
        newChatRoom(
            withChatId = 2L,
            withPeerCount = 0L,
            withPeerHandlesList = listOf(firstHandle, lastHandle),
            withPeerPrivilegesList = emptyList(),
            withIsGroup = true,
        )
    )

    private val noPeerHandleRoom = listOf(
        newChatRoom(
            withChatId = 3L,
            withPeerCount = 1L,
            withPeerHandlesList = emptyList(),
            withPeerPrivilegesList = emptyList(),
            withIsGroup = true,
        )
    )

    private val validRooms = listOf(
        newChatRoom(
            withChatId = 5L,
            withPeerCount = 1L,
            withPeerHandlesList = listOf(firstHandle, lastHandle),
            withPeerPrivilegesList = emptyList(),
            withIsGroup = true,
            withTitle = "Z room",
            withIsPublic = true,
        ),
        newChatRoom(
            withChatId = 4L,
            withPeerCount = 1L,
            withPeerHandlesList = listOf(firstHandle, lastHandle),
            withPeerPrivilegesList = emptyList(),
            withIsGroup = true,
            withTitle = "A room",
            withIsPublic = true,
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        underTest = GetContactGroupsUseCase(
            chatRepository = chatRepository,
            avatarRepository = avatarRepository,
            contactsRepository = contactsRepository,
            UnconfinedTestDispatcher()
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            chatRepository,
            avatarRepository,
            contactsRepository,
        )
    }

    @Test
    fun `test that empty list is returned if chat room is no group`() = runTest {
        whenever(chatRepository.getChatRooms()).thenReturn(noGroupRoom)
        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that empty list is returned if chat room has no peers`() = runTest {
        whenever(chatRepository.getChatRooms()).thenReturn(noPeersRoom)
        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that empty list is returned if chat room has no peer handles`() = runTest {
        whenever(chatRepository.getChatRooms()).thenReturn(noPeerHandleRoom)
        assertThat(underTest()).isEmpty()
    }

    @Test
    fun `test that alphabetically sorted list is returned if chat rooms are valid`() = runTest {
        val email = "mail"
        val name = "name"
        val file = "avatar_file"
        val color = 69
        whenever(chatRepository.getChatRooms()).thenReturn(validRooms)
        whenever(contactsRepository.getUserEmail(any(), any())).thenReturn(email)
        whenever(contactsRepository.getUserFirstName(any(), any(), any())).thenReturn(name)
        whenever(avatarRepository.getAvatarFile(anyLong(), any())).thenReturn(File(file))
        whenever(avatarRepository.getAvatarColor(any())).thenReturn(color)

        val expectedGroups = listOf(
            ContactGroupItem(
                chatId = 4L,
                title = "A room",
                firstUser = ContactGroupUser(
                    handle = firstHandle,
                    email = email,
                    firstName = name,
                    avatar = file.toUri(),
                    avatarColor = color,
                ),
                lastUser = ContactGroupUser(
                    handle = lastHandle,
                    email = email,
                    firstName = name,
                    avatar = file.toUri(),
                    avatarColor = color,
                ),
                isPublic = true,
            ),
            ContactGroupItem(
                chatId = 5L,
                title = "Z room",
                firstUser = ContactGroupUser(
                    handle = firstHandle,
                    email = email,
                    firstName = name,
                    avatar = file.toUri(),
                    avatarColor = color,
                ),
                lastUser = ContactGroupUser(
                    handle = lastHandle,
                    email = email,
                    firstName = name,
                    avatar = file.toUri(),
                    avatarColor = color,
                ),
                isPublic = true,
            )
        )
        assertThat(underTest()).isEqualTo(expectedGroups)
    }
}
