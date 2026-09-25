package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAnonymousModeUseCaseTest {

    lateinit var underTest: IsAnonymousModeUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = IsAnonymousModeUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = "if init state is: {0}")
    @EnumSource(ChatInitState::class)
    fun `test that is anonymous mode returns correctly`(
        initState: ChatInitState,
    ) = runTest {
        whenever(chatRepository.getChatInitState()).thenReturn(initState)
        Truth.assertThat(underTest()).isEqualTo(initState == ChatInitState.ANONYMOUS)
    }
}