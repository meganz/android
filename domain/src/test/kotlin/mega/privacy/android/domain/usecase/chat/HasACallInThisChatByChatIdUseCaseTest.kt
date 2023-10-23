package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasACallInThisChatByChatIdUseCaseTest {

    private lateinit var underTest: HasACallInThisChatByChatIdUseCase
    private val chatRepository: ChatRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = HasACallInThisChatByChatIdUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = " returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that has a call in this chat returns correctly if repository`(
        hasACallInThisChat: Boolean,
    ) = runTest {
        val chatId = 123L
        whenever(chatRepository.hasCallInChatRoom(chatId)).thenReturn(hasACallInThisChat)
        Truth.assertThat(underTest.invoke(chatId)).isEqualTo(hasACallInThisChat)
    }
}