package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EnableOrDisableAudioUseCaseTest {
    private val callRepository: CallRepository = mock()
    private val chatId = 123L

    private lateinit var underTest: EnableOrDisableAudioUseCase

    @BeforeEach
    fun setup() {
        underTest = EnableOrDisableAudioUseCase(callRepository)
    }

    @Test
    fun `test that enableAudio is called when enable is true`() = runTest {
        underTest(chatId, true)
        verify(callRepository).enableAudio(chatId)
        verifyNoMoreInteractions(callRepository)
    }

    @Test
    fun `test that disableAudio is called when enable is false`() = runTest {
        underTest(chatId, false)
        verify(callRepository).disableAudio(chatId)
        verifyNoMoreInteractions(callRepository)
    }
}