package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetChatBackgroundStatusUseCaseTest {
    private lateinit var underTest: SetChatBackgroundStatusUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetChatBackgroundStatusUseCase(
            chatRepository = chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = "inBackground: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke sets the background status when it differs from the current one`(
        inBackground: Boolean,
    ) = runTest {
        whenever(chatRepository.isChatInBackground()).thenReturn(!inBackground)

        underTest(inBackground)

        verify(chatRepository).setChatBackgroundStatus(inBackground)
    }

    @ParameterizedTest(name = "inBackground: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke does not set the background status when it matches the current one`(
        inBackground: Boolean,
    ) = runTest {
        whenever(chatRepository.isChatInBackground()).thenReturn(inBackground)

        underTest(inBackground)

        verify(chatRepository, never()).setChatBackgroundStatus(any())
    }

    @ParameterizedTest(name = "inBackground: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that invoke does not set the background status when the current one is unavailable`(
        inBackground: Boolean,
    ) = runTest {
        whenever(chatRepository.isChatInBackground()).thenReturn(null)

        underTest(inBackground)

        verify(chatRepository, never()).setChatBackgroundStatus(any())
    }
}
