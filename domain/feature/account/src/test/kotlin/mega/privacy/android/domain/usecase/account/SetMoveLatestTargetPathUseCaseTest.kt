package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetMoveLatestTargetPathUseCaseTest {
    private val accountRepository = mock<AccountRepository>()
    private val underTest = SetMoveLatestTargetPathUseCase(accountRepository)

    @BeforeEach
    fun reset() {
        reset(accountRepository)
    }

    @Test
    fun `test that invoke calls repository setLatestTargetMovePreference`() = runTest {
        val path = 1234L
        underTest(path)
        verify(accountRepository).setLatestTargetMovePreference(path)
    }
}
