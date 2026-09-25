package mega.privacy.android.domain.usecase.meeting.raisehandtospeak

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RaiseHandToSpeakUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: RaiseHandToSpeakUseCase

    @BeforeEach
    fun setup() {
        underTest = RaiseHandToSpeakUseCase(callRepository)
    }

    @Test
    fun `test that raise hand to speak is called with correct parameters`() = runTest {
        val chatId = 123L

        underTest.invoke(chatId)

        verify(callRepository).raiseHandToSpeak(chatId)
    }
}