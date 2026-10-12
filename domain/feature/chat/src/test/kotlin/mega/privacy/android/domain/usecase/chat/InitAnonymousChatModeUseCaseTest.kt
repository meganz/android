package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitAnonymousChatModeUseCaseTest {

    private lateinit var underTest: InitAnonymousChatModeUseCase

    private val chatRepository = mock<ChatRepository>()


    @BeforeAll
    fun setup() {
        underTest = InitAnonymousChatModeUseCase(chatRepository = chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest
    @EnumSource(ChatInitState::class)
    fun `test that use case behaves correctly for each ChatInitState when initAnonymousChat is not error`(
        chatInitState: ChatInitState,
    ) = runTest {
        whenever(chatRepository.getChatInitState()) doReturn chatInitState

        if (chatInitState < ChatInitState.WAITING_NEW_SESSION) {
            whenever(chatRepository.initAnonymousChat()) doReturn ChatInitState.ANONYMOUS
        }

        underTest.invoke()

        verify(chatRepository).getChatInitState()

        if (chatInitState < ChatInitState.WAITING_NEW_SESSION) {
            verify(chatRepository).initAnonymousChat()
        } else {
            verifyNoMoreInteractions(chatRepository)
        }
    }

    @ParameterizedTest
    @EnumSource(ChatInitState::class)
    fun `test that use case throws ChatNotInitializedErrorStatus for each ChatInitState when initAnonymousChat is error, only when corresponds `(
        chatInitState: ChatInitState,
    ) = runTest {
        whenever(chatRepository.getChatInitState()) doReturn chatInitState

        if (chatInitState < ChatInitState.WAITING_NEW_SESSION) {
            whenever(chatRepository.initAnonymousChat()) doReturn ChatInitState.ERROR
        }

        runCatching { underTest.invoke() }
            .onFailure { assertThat(it).isInstanceOf(ChatNotInitializedErrorStatus::class.java) }
            .onSuccess { assertThat(it).isInstanceOf(Unit::class.java) }
    }
}