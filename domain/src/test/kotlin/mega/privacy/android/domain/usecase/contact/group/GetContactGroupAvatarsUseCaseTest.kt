package mega.privacy.android.domain.usecase.contact.group

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContactGroupAvatarsUseCaseTest {

    private lateinit var underTest: GetContactGroupAvatarsUseCase

    private val chatParticipantsRepository = mock<ChatParticipantsRepository>()
    private val accountRepository = mock<AccountRepository>()
    private val avatarRepository = mock<AvatarRepository>()
    private val getUserFirstName = mock<GetUserFirstName>()

    private val myHandle = 100L
    private val firstHandle = 666L
    private val secondHandle = 777L

    @BeforeEach
    fun setUp() {
        underTest = GetContactGroupAvatarsUseCase(
            chatParticipantsRepository = chatParticipantsRepository,
            accountRepository = accountRepository,
            avatarRepository = avatarRepository,
            getUserFirstName = getUserFirstName,
        )
        stubAccount()
        stubAvatars()
        getUserFirstName.stub {
            on { invoke(any(), any(), any()) } doReturn "First"
        }
    }

    @AfterEach
    fun tearDown() {
        reset(
            chatParticipantsRepository,
            accountRepository,
            avatarRepository,
            getUserFirstName,
        )
    }

    private fun stubAccount() {
        accountRepository.stub {
            on { getUserAccount() } doReturn UserAccount(
                userId = UserId(myHandle),
                email = "me@mega.co.nz",
                fullName = "Me",
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = AccountType.FREE,
            )
        }
    }

    private fun stubAvatars() {
        avatarRepository.stub {
            on { getMyAvatarFile(any<Boolean>()) } doReturn File("my_avatar")
            on { getAvatarFile(any<Long>(), any<Boolean>()) } doReturn File("avatar")
            on { getAvatarColor(any<Long>()) } doReturn AVATAR_COLOR
        }
    }

    @Test
    fun `test that account is fetched only once for multiple groups`() = runTest {
        val rooms = listOf(
            chatRoom(chatId = 1L, title = "Alpha"),
            chatRoom(chatId = 2L, title = "Beta"),
        )
        whenever(chatParticipantsRepository.getChatParticipantsHandles(any(), any()))
            .thenReturn(listOf(firstHandle))

        underTest(rooms)

        verify(accountRepository, times(1)).getUserAccount()
    }

    @Test
    fun `test that a user in multiple groups is resolved only once`() = runTest {
        val rooms = listOf(
            chatRoom(chatId = 1L, title = "Alpha"),
            chatRoom(chatId = 2L, title = "Beta"),
        )
        whenever(chatParticipantsRepository.getChatParticipantsHandles(any(), any()))
            .thenReturn(listOf(firstHandle))

        underTest(rooms)

        verify(getUserFirstName, times(1)).invoke(eq(firstHandle), any(), any())
        verify(avatarRepository, times(1)).getAvatarFile(eq(firstHandle), any())
        verify(avatarRepository, times(1)).getAvatarColor(eq(firstHandle))
    }

    @Test
    fun `test that only the title placeholder is returned when the room is inactive`() = runTest {
        val rooms = listOf(chatRoom(chatId = 1L, title = "Alpha", isActive = false))
        whenever(chatParticipantsRepository.getChatParticipantsHandles(any(), any()))
            .thenReturn(listOf(firstHandle, secondHandle))

        val result = underTest(rooms)

        assertThat(result[1L]).containsExactly(ChatAvatarItem(placeholderText = "Alpha"))
    }

    @Test
    fun `test that only the title placeholder is returned when there are no participants`() =
        runTest {
            val rooms = listOf(chatRoom(chatId = 1L, title = "Alpha"))
            whenever(chatParticipantsRepository.getChatParticipantsHandles(any(), any()))
                .thenReturn(emptyList())

            val result = underTest(rooms)

            assertThat(result[1L]).containsExactly(ChatAvatarItem(placeholderText = "Alpha"))
        }

    @Test
    fun `test that self and participant avatars are returned when there is one participant`() =
        runTest {
            val rooms = listOf(chatRoom(chatId = 1L, title = "Alpha"))
            whenever(chatParticipantsRepository.getChatParticipantsHandles(any(), any()))
                .thenReturn(listOf(firstHandle))

            val result = underTest(rooms)

            assertThat(result[1L]).containsExactly(
                ChatAvatarItem(
                    placeholderText = "Me",
                    uri = File("my_avatar").absolutePath,
                    color = AVATAR_COLOR,
                ),
                ChatAvatarItem(
                    placeholderText = "First",
                    uri = File("avatar").absolutePath,
                    color = AVATAR_COLOR,
                ),
            ).inOrder()
        }

    @Test
    fun `test that self avatar is used for the current user handle among participants`() = runTest {
        val rooms = listOf(chatRoom(chatId = 1L, title = "Alpha"))
        whenever(chatParticipantsRepository.getChatParticipantsHandles(any(), any()))
            .thenReturn(listOf(myHandle, firstHandle))

        val result = underTest(rooms)

        assertThat(result[1L]).containsExactly(
            ChatAvatarItem(
                placeholderText = "Me",
                uri = File("my_avatar").absolutePath,
                color = AVATAR_COLOR,
            ),
            ChatAvatarItem(
                placeholderText = "First",
                uri = File("avatar").absolutePath,
                color = AVATAR_COLOR,
            ),
        ).inOrder()
    }

    private fun chatRoom(
        chatId: Long,
        title: String,
        isActive: Boolean = true,
    ) = ChatRoom(
        chatId = chatId,
        ownPrivilege = ChatRoomPermission.Standard,
        numPreviewers = 0L,
        peerPrivilegesByHandles = emptyMap(),
        peerCount = 2L,
        peerHandlesList = emptyList(),
        peerPrivilegesList = emptyList(),
        isGroup = true,
        isPublic = true,
        isPreview = false,
        authorizationToken = null,
        title = title,
        hasCustomTitle = false,
        unreadCount = 0,
        userTyping = 0L,
        userHandle = 0L,
        isActive = isActive,
        isArchived = false,
        retentionTime = 0L,
        creationTime = 0L,
        isMeeting = false,
        isWaitingRoom = false,
        isOpenInvite = false,
        isSpeakRequest = false,
        isNoteToSelf = false,
    )

    private companion object {
        const val AVATAR_COLOR = 69
    }
}
