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
internal class EnableOrDisableVideoUseCaseTest {
    private val callRepository: CallRepository = mock()
    private val chatId = 123L

    private lateinit var underTest: EnableOrDisableVideoUseCase

    @BeforeEach
    fun setup() {
        underTest = EnableOrDisableVideoUseCase(callRepository)
    }

    @Test
    fun `test that enableVideo is called when enable is true`() = runTest {
        underTest(chatId, true)
        verify(callRepository).enableVideo(chatId)
        verifyNoMoreInteractions(callRepository)
    }

    @Test
    fun `test that disableVideo is called when enable is false`() = runTest {
        underTest(chatId, false)
        verify(callRepository).disableVideo(chatId)
        verifyNoMoreInteractions(callRepository)
    }
}