package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.call.SetIgnoredCallUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetIgnoredCallUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: SetIgnoredCallUseCase

    @BeforeEach
    fun setup() {
        underTest = SetIgnoredCallUseCase(callRepository)
    }

    @Test
    fun `test that set ignored call is called with correct parameters`() = runTest {
        val chatId = 123L

        underTest.invoke(chatId)

        verify(callRepository).setIgnoredCall(chatId)
    }
}