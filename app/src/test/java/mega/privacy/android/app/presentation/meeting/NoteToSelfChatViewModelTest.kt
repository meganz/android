package mega.privacy.android.app.presentation.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorNoteToSelfChatIsEmptyUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NoteToSelfChatViewModelTest {
    private lateinit var underTest: NoteToSelfChatViewModel
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase = mock()
    private val monitorNoteToSelfChatIsEmptyUseCase: MonitorNoteToSelfChatIsEmptyUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            getNoteToSelfChatUseCase,
            monitorNoteToSelfChatIsEmptyUseCase,
        )
    }

    private fun initTestClass() {
        underTest = NoteToSelfChatViewModel(
            getNoteToSelfChatUseCase = getNoteToSelfChatUseCase,
            monitorNoteToSelfChatIsEmptyUseCase = monitorNoteToSelfChatIsEmptyUseCase,
        )
    }

    @Test
    fun `test that noteToSelfChatId updated when calling getNoteToSelfChatUseCase`() = runTest {
        val chatRoom = mock<ChatRoom> {
            on { chatId }.thenReturn(123L)
            on { isNoteToSelf }.thenReturn(true)
        }
        whenever(getNoteToSelfChatUseCase()).thenReturn(chatRoom)
        whenever(monitorNoteToSelfChatIsEmptyUseCase(123L)).thenReturn(
            flowOf(true)
        )
        initTestClass()
        underTest.state.test {
            val updatedState = awaitItem()
            Truth.assertThat(updatedState.noteToSelfChatId).isEqualTo(chatRoom.chatId)
            Truth.assertThat(updatedState.isNoteToSelfChatEmpty).isTrue()
        }
    }

}