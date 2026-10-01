package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.account.QueryCancelLinkException
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
 * Test class for [QueryCancelLinkUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QueryCancelLinkUseCaseTest {
    private lateinit var underTest: QueryCancelLinkUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = QueryCancelLinkUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that the account cancellation link is returned when the use case is invoked`() =
        runTest {
            val expectedLink = "expected/link"
            whenever(accountRepository.queryCancelLink(any())).thenReturn(expectedLink)

            val actualLink = underTest.invoke("link/to/query")
            assertThat(actualLink).isEqualTo(expectedLink)
        }

    @Test
    fun `test that an unrelated account cancellation link exception is thrown`() = runTest {
        whenever(accountRepository.queryCancelLink(any())).thenAnswer {
            throw QueryCancelLinkException.UnrelatedAccountCancellationLink(
                errorCode = 20,
                errorString = "The link is unrelated to this account"
            )
        }

        assertThrows<QueryCancelLinkException.UnrelatedAccountCancellationLink> {
            underTest.invoke("link/to/query")
        }
    }

    @Test
    fun `test that an expired account cancellation link exception is thrown`() = runTest {
        whenever(accountRepository.queryCancelLink(any())).thenAnswer {
            throw QueryCancelLinkException.ExpiredAccountCancellationLink(
                errorCode = 25,
                errorString = "The link has expired"
            )
        }

        assertThrows<QueryCancelLinkException.ExpiredAccountCancellationLink> {
            underTest.invoke("link/to/query")
        }
    }

    @Test
    fun `test that an unknown exception is thrown`() = runTest {
        whenever(accountRepository.queryCancelLink(any())).thenAnswer {
            throw QueryCancelLinkException.Unknown(
                errorCode = 30,
                errorString = "An unexpected issue occurred"
            )
        }

        assertThrows<QueryCancelLinkException.Unknown> {
            underTest.invoke("link/to/query")
        }
    }
}