package mega.privacy.android.domain.usecase.chat.participants

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorChatParticipantsUseCaseTest {

    private lateinit var underTest: MonitorChatParticipantsUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatParticipantsRepository = mock<ChatParticipantsRepository>()
    private val contactsRepository = mock<ContactsRepository>()
    private val avatarRepository = mock<AvatarRepository>()
    private val requestUserLastGreenUseCase = mock<RequestUserLastGreenUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatParticipantsUseCase(
            chatRepository = chatRepository,
            chatParticipantsRepository = chatParticipantsRepository,
            contactsRepository = contactsRepository,
            avatarRepository = avatarRepository,
            requestUserLastGreenUseCase = requestUserLastGreenUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            chatRepository,
            chatParticipantsRepository,
            contactsRepository,
            avatarRepository,
            requestUserLastGreenUseCase,
        )
    }

    @Test
    fun `test that every participant is enriched with its own requested info`() = runTest {
        val participants = (0..2).map { participant(handle = it.toLong()) }
        stubMonitors()
        whenever(
            chatParticipantsRepository.getAllChatParticipants(
                chatId = CHAT_ID,
                preloadUserAttributes = true,
            )
        ).thenReturn(participants)

        participants.forEachIndexed { index, participant ->
            whenever(chatParticipantsRepository.getStatus(participant))
                .thenReturn(STATUSES[index])
            whenever(chatParticipantsRepository.areCredentialsVerified(participant))
                .thenReturn(index % 2 == 0)
            whenever(chatParticipantsRepository.getAvatarColor(participant))
                .thenReturn(BASE_COLOR + index)
            whenever(chatParticipantsRepository.getAlias(participant))
                .thenReturn("alias$index")
            whenever(chatParticipantsRepository.getAvatarUri(participant))
                .thenReturn(File("/avatars/$index.jpg"))
        }

        // The first emission is the unenriched list, the second carries the requested info.
        val actual = underTest(CHAT_ID).drop(1).first()

        assertThat(actual.map { it.handle }).containsExactly(0L, 1L, 2L).inOrder()
        actual.forEachIndexed { index, participant ->
            assertThat(participant.status).isEqualTo(STATUSES[index])
            assertThat(participant.areCredentialsVerified).isEqualTo(index % 2 == 0)
            assertThat(participant.defaultAvatarColor).isEqualTo(BASE_COLOR + index)
            assertThat(participant.data.alias).isEqualTo("alias$index")
            assertThat(participant.data.avatarUri).isEqualTo(File("/avatars/$index.jpg").toString())
        }
    }

    @Test
    fun `test that a null avatar uri leaves the participant avatar uri null`() = runTest {
        val participant = participant(handle = 7L)
        stubMonitors()
        whenever(
            chatParticipantsRepository.getAllChatParticipants(
                chatId = CHAT_ID,
                preloadUserAttributes = true,
            )
        ).thenReturn(listOf(participant))
        whenever(chatParticipantsRepository.getStatus(participant))
            .thenReturn(UserChatStatus.Online)
        whenever(chatParticipantsRepository.areCredentialsVerified(participant)).thenReturn(false)
        whenever(chatParticipantsRepository.getAvatarColor(participant)).thenReturn(BASE_COLOR)
        whenever(chatParticipantsRepository.getAlias(participant)).thenReturn(null)
        whenever(chatParticipantsRepository.getAvatarUri(participant)).thenReturn(null)

        val actual = underTest(CHAT_ID).drop(1).first()

        assertThat(actual).hasSize(1)
        assertThat(actual.first().data.avatarUri).isNull()
        assertThat(actual.first().data.alias).isNull()
    }

    private suspend fun stubMonitors() {
        whenever(contactsRepository.monitorChatPresenceLastGreenUpdates())
            .thenReturn(emptyFlow())
        whenever(contactsRepository.monitorChatOnlineStatusUpdates()).thenReturn(emptyFlow())
        whenever(contactsRepository.monitorContactUpdates()).thenReturn(emptyFlow())
        whenever(chatRepository.monitorChatListItemUpdates()).thenReturn(emptyFlow())
        whenever(avatarRepository.monitorMyAvatarFile()).thenReturn(emptyFlow())
        whenever(chatRepository.monitorMyName()).thenReturn(emptyFlow())
        whenever(chatRepository.monitorMyEmail()).thenReturn(emptyFlow())
    }

    private fun participant(handle: Long) = ChatParticipant(
        handle = handle,
        data = ContactData(
            fullName = "Full name $handle",
            alias = null,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        email = "user$handle@mega.co.nz",
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        defaultAvatarColor = 0,
    )

    private companion object {
        const val CHAT_ID = 123L
        const val BASE_COLOR = 100
        val STATUSES = listOf(
            UserChatStatus.Online,
            UserChatStatus.Away,
            UserChatStatus.Busy,
        )
    }
}
