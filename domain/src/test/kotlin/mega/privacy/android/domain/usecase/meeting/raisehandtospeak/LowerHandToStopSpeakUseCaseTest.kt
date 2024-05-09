package mega.privacy.android.domain.usecase.meeting.raisehandtospeak

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LowerHandToStopSpeakUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: LowerHandToStopSpeakUseCase

    @BeforeEach
    fun setup() {
        underTest = LowerHandToStopSpeakUseCase(callRepository)
    }

    @Test
    fun `test that lower hand to stop speak is called with correct parameters`() = runTest {
        val chatId = 123L

        underTest.invoke(chatId)

        verify(callRepository).lowerHandToStopSpeak(chatId)
    }
}