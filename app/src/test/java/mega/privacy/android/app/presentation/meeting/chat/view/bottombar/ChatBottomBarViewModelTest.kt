package mega.privacy.android.app.presentation.meeting.chat.view.bottombar

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.chat.SetChatDraftMessageUseCase
import mega.privacy.android.domain.usecase.chat.SetUserTypingStatusUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatBottomBarViewModelTest {
    private lateinit var underTest: ChatBottomBarViewModel
    private val setChatDraftMessageUseCase = mock<SetChatDraftMessageUseCase>()
    private val setUserTypingStatusUseCase = mock<SetUserTypingStatusUseCase>()
    private val applicationScope: CoroutineScope = CoroutineScope(dispatcher)
    private val chatId = 123L

    private val savedStateHandle: SavedStateHandle = SavedStateHandle(
        mapOf(
            "chatId" to chatId.toString(),
            "chatAction" to Constants.ACTION_CHAT_SHOW_MESSAGES,
        )
    )

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    private fun initTestClass() {
        underTest = ChatBottomBarViewModel(
            setChatDraftMessageUseCase = setChatDraftMessageUseCase,
            setUserTypingStatusUseCase = setUserTypingStatusUseCase,
            applicationScope = applicationScope,
            savedStateHandle = savedStateHandle,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setChatDraftMessageUseCase,
            setUserTypingStatusUseCase,
        )
    }

    @Test
    fun `test that save draft message invoke use case`() = runTest {
        val draftMessage = "draft message"
        val editingMessageId = 456L
        underTest.saveDraftMessage(draftMessage, editingMessageId)
        verify(setChatDraftMessageUseCase).invoke(
            chatId = chatId,
            draftMessage = draftMessage,
            editingMessageId = editingMessageId
        )
    }

    @Test
    fun `test that calls to onUserTyping calls the use case with true`() = runTest {
        underTest.onUserTyping()
        verify(setUserTypingStatusUseCase).invoke(true, chatId)
    }

    @Test
    fun `test that calls to onExitTypingContext calls the typing use case with false`() = runTest {
        underTest.onExitTypingContext()
        verify(setUserTypingStatusUseCase).invoke(false, chatId)
    }

    companion object {
        private val dispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(dispatcher)
    }
}