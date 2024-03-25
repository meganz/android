package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.account.QueryChangeEmailLinkException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [QueryChangeEmailLinkUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QueryChangeEmailLinkUseCaseTest {
    private lateinit var underTest: QueryChangeEmailLinkUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = QueryChangeEmailLinkUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that the change email link is returned when the use case is invoked`() = runTest {
        val expectedLink = "expected/email/link"
        whenever(accountRepository.queryChangeEmailLink(any())).thenReturn(expectedLink)

        val actualLink = underTest.invoke("link/to/query")
        assertThat(actualLink).isEqualTo(expectedLink)
    }

    @Test
    fun `test that a link not generated exception is thrown`() = runTest {
        whenever(accountRepository.queryChangeEmailLink(any())).thenAnswer {
            throw QueryChangeEmailLinkException.LinkNotGenerated(
                errorCode = 20,
                errorString = "The link was not generated"
            )
        }

        assertThrows<QueryChangeEmailLinkException.LinkNotGenerated> {
            underTest.invoke("email/link/to/query")
        }
    }

    @Test
    fun `test that an unknown exception is thrown`() = runTest {
        whenever(accountRepository.queryChangeEmailLink(any())).thenAnswer {
            throw QueryChangeEmailLinkException.Unknown(
                errorCode = 30,
                errorString = "An unexpected issue occurred"
            )
        }

        assertThrows<QueryChangeEmailLinkException.Unknown> {
            underTest.invoke("email/link/to/query")
        }
    }
}