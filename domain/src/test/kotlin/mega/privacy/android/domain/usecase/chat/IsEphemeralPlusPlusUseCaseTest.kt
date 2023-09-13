package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsEphemeralPlusPlusUseCaseTest {
    private val accountRepository: AccountRepository = mock()

    private lateinit var underTest: IsEphemeralPlusPlusUseCase

    @BeforeEach
    fun setup() {
        underTest = IsEphemeralPlusPlusUseCase(accountRepository)
    }

    @Test
    fun `test that isEphemeralPlusPlus is called when use case is invoked`() = runTest {
        underTest.invoke()

        verify(accountRepository).isEphemeralPlusPlus()
        verifyNoMoreInteractions(accountRepository)
    }
}
