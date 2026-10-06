package mega.privacy.android.feature.contact.conversation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorNoteToSelfChatIsEmptyUseCase
import mega.privacy.android.feature.contact.conversation.model.NoteToSelfUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions

@ExtendWith(CoroutineMainDispatcherExtension::class)
class NoteToSelfViewModelTest {
    private lateinit var underTest: NoteToSelfViewModel

    private val getNoteToSelfChatUseCase = mock<GetNoteToSelfChatUseCase>()
    private val monitorNoteToSelfChatIsEmptyUseCase = mock<MonitorNoteToSelfChatIsEmptyUseCase>()

    private val chatId = 123L
    private val chatRoom = mock<ChatRoom> {
        on { this.chatId } doReturn chatId
    }

    @BeforeEach
    fun setUp() {
        underTest = NoteToSelfViewModel(
            getNoteToSelfChatUseCase = getNoteToSelfChatUseCase,
            monitorNoteToSelfChatIsEmptyUseCase = monitorNoteToSelfChatIsEmptyUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getNoteToSelfChatUseCase,
            monitorNoteToSelfChatIsEmptyUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(NoteToSelfUiState.Loading)
    }

    @Test
    fun `test that state is a hint when the note to self chat is empty`() = runTest {
        stubChat(isEmpty = true)

        underTest.uiState.test {
            assertThat(awaitDataState()).isEqualTo(
                NoteToSelfUiState.Data(chatId = chatId, isHint = true)
            )
        }
    }

    @Test
    fun `test that state is not a hint when the note to self chat has messages`() = runTest {
        stubChat(isEmpty = false)

        underTest.uiState.test {
            assertThat(awaitDataState()).isEqualTo(
                NoteToSelfUiState.Data(chatId = chatId, isHint = false)
            )
        }
    }

    @Test
    fun `test that state updates when the note to self chat emptiness changes`() = runTest {
        val isEmpty = MutableStateFlow(true)
        getNoteToSelfChatUseCase.stub { onBlocking { invoke() } doReturn chatRoom }
        monitorNoteToSelfChatIsEmptyUseCase.stub {
            on { invoke(chatId) } doReturn isEmpty
        }

        underTest.uiState.test {
            assertThat(awaitDataState().isHint).isTrue()
            isEmpty.value = false
            assertThat((awaitItem() as NoteToSelfUiState.Data).isHint).isFalse()
        }
    }

    @Test
    fun `test that state stays Loading when no note to self chat is returned`() = runTest {
        getNoteToSelfChatUseCase.stub { onBlocking { invoke() } doReturn null }

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(NoteToSelfUiState.Loading)
            expectNoEvents()
        }
        verifyNoInteractions(monitorNoteToSelfChatIsEmptyUseCase)
    }

    @Test
    fun `test that state stays Loading when getting the note to self chat fails`() = runTest {
        getNoteToSelfChatUseCase.stub { onBlocking { invoke() } doThrow RuntimeException() }

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(NoteToSelfUiState.Loading)
            expectNoEvents()
        }
        verifyNoInteractions(monitorNoteToSelfChatIsEmptyUseCase)
    }

    private fun stubChat(isEmpty: Boolean) {
        getNoteToSelfChatUseCase.stub { onBlocking { invoke() } doReturn chatRoom }
        monitorNoteToSelfChatIsEmptyUseCase.stub {
            on { invoke(chatId) } doReturn flow {
                emit(isEmpty)
                awaitCancellation()
            }
        }
    }

    private suspend fun ReceiveTurbine<NoteToSelfUiState>.awaitDataState(): NoteToSelfUiState.Data {
        var item = awaitItem()
        while (item !is NoteToSelfUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}
