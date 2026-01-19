package mega.privacy.android.domain.usecase.transfers.uploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatPitagTargetUseCaseTest {

    private lateinit var underTest: GetChatPitagTargetUseCase

    private val chatRepository = mock<ChatRepository>()

    private val chatId = 1234L
    private val pendingMessage = mock<PendingMessage> {
        on { this.chatId } doReturn chatId
    }
    private val pendingMessages = listOf(pendingMessage)

    @BeforeAll
    fun setup() {
        underTest = GetChatPitagTargetUseCase(chatRepository = chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that NotApplicable is returned if pending messages list is empty`() = runTest {
        assertThat(underTest(emptyList())).isEqualTo(PitagTarget.NotApplicable)
    }

    @Test
    fun `test that MultipleChats is returned if pending messages list contains more than one items`() =
        runTest {
            val pendingMessage1 = mock<PendingMessage>()
            val pendingMessage2 = mock<PendingMessage>()
            val pendingMessages = listOf(pendingMessage1, pendingMessage2)

            assertThat(underTest(pendingMessages)).isEqualTo(PitagTarget.MultipleChats)
        }

    @Test
    fun `test that NoteToSelf is returned if pending messages list contains only one item and is the note to self`() =
        runTest {
            whenever(chatRepository.isNoteToSelfChat(chatId)) doReturn true

            assertThat(underTest(pendingMessages)).isEqualTo(PitagTarget.NoteToSelf)
        }

    @Test
    fun `test that ChatGroup is returned if pending messages list contains only one item and is a group`() =
        runTest {
            whenever(chatRepository.isNoteToSelfChat(chatId)) doReturn false
            whenever(chatRepository.isGroupChat(chatId)) doReturn true

            assertThat(underTest(pendingMessages)).isEqualTo(PitagTarget.ChatGroup)
        }

    @Test
    fun `test that Chat1To1 is returned if pending messages list contains only one item and is a group`() =
        runTest {
            whenever(chatRepository.isNoteToSelfChat(chatId)) doReturn false
            whenever(chatRepository.isGroupChat(chatId)) doReturn false

            assertThat(underTest(pendingMessages)).isEqualTo(PitagTarget.Chat1To1)
        }

    @Test
    fun `test that NotApplicable is returned if pending messages list contains only one item but repository call returns null`() =
        runTest {
            whenever(chatRepository.isNoteToSelfChat(chatId)) doReturn false
            whenever(chatRepository.isGroupChat(chatId)) doReturn null

            assertThat(underTest(pendingMessages)).isEqualTo(PitagTarget.NotApplicable)
        }
}