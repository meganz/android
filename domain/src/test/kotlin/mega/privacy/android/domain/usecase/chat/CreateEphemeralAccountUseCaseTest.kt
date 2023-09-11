package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateEphemeralAccountUseCaseTest {
    private val chatRepository: ChatRepository = mock()

    private lateinit var underTest: CreateEphemeralAccountUseCase

    @BeforeEach
    fun setup() {
        underTest = CreateEphemeralAccountUseCase(chatRepository)
    }

    @Test
    fun `test that createEphemeralAccountPlusPlus is called with correct parameters`() = runTest {
        val firstName = "John"
        val lastName = "Doe"

        underTest.invoke(firstName, lastName)

        verify(chatRepository).createEphemeralAccountPlusPlus(firstName, lastName)
        verifyNoMoreInteractions(chatRepository)
    }
}
