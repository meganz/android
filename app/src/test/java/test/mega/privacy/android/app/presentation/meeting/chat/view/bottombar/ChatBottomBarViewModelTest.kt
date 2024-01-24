package test.mega.privacy.android.app.presentation.meeting.chat.view.bottombar

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.meeting.chat.view.bottombar.ChatBottomBarViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.chat.SetChatDraftMessageUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatBottomBarViewModelTest {
    private lateinit var underTest: ChatBottomBarViewModel
    private val setChatDraftMessageUseCase: SetChatDraftMessageUseCase = mock()
    private val dispatcher = UnconfinedTestDispatcher()
    private val applicationScope: CoroutineScope = CoroutineScope(dispatcher)
    private val chatId = 123L
    private val savedStateHandle: SavedStateHandle = mock {
        on { get<Long>(Constants.CHAT_ID) } doReturn chatId
    }

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(dispatcher)
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setChatDraftMessageUseCase,
            savedStateHandle,
        )
    }

    @Test
    fun `test that save draft message invoke use case`() = runTest {
        val draftMessage = "draft message"
        underTest.saveDraftMessage(draftMessage)
        verify(setChatDraftMessageUseCase).invoke(
            chatId = chatId,
            draftMessage = draftMessage
        )
    }

    private fun initTestClass() {
        underTest = ChatBottomBarViewModel(
            setChatDraftMessageUseCase,
            applicationScope,
            savedStateHandle,
        )
    }
}