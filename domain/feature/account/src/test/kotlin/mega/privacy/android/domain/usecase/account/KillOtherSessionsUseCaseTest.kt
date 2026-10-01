package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test Class for [KillOtherSessionsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KillOtherSessionsUseCaseTest {
    private lateinit var underTest: KillOtherSessionsUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = KillOtherSessionsUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that kill other sessions is invoked`() = runTest {
        underTest.invoke()
        verify(accountRepository).killOtherSessions()
    }
}