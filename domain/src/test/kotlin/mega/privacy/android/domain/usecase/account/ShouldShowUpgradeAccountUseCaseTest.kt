package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [ShouldShowUpgradeAccountUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShouldShowUpgradeAccountUseCaseTest {
    private lateinit var underTest: ShouldShowUpgradeAccountUseCase

    private val accountRepository = mock<AccountRepository>()
    private val getSpecificAccountDetailUseCase = mock<GetSpecificAccountDetailUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = ShouldShowUpgradeAccountUseCase(
            getSpecificAccountDetailUseCase = getSpecificAccountDetailUseCase,
            accountRepository = accountRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository, getSpecificAccountDetailUseCase)
    }

    @Test
    fun `test that returns false when user has logged in before`() = runTest {
        whenever(accountRepository.hasUserLoggedInBefore()).thenReturn(true)

        val result = underTest.invoke()

        assertThat(result).isFalse()
        verify(getSpecificAccountDetailUseCase, never()).invoke(any(), any(), any())
    }

    @Test
    fun `test that returns true when user has not logged in before and account type is FREE`() =
        runTest {
            val userId = UserId(123L)
            val levelDetail = mock<AccountLevelDetail> {
                on { accountType }.thenReturn(AccountType.FREE)
            }
            val accountDetail = mock<AccountDetail> {
                on { this.levelDetail }.thenReturn(levelDetail)
            }

            whenever(accountRepository.hasUserLoggedInBefore()).thenReturn(false)
            whenever(accountRepository.getLoggedInUserId()).thenReturn(userId)
            whenever(
                getSpecificAccountDetailUseCase(
                    storage = false,
                    transfer = false,
                    pro = true
                )
            ).thenReturn(accountDetail)

            val result = underTest.invoke()

            assertThat(result).isTrue()
            verify(accountRepository).addLoggedInUserHandle(userId.id)
        }

    @Test
    fun `test that returns false when user has not logged in before and account type is not FREE`() =
        runTest {
            val userId = UserId(123L)
            val levelDetail = mock<AccountLevelDetail> {
                on { accountType }.thenReturn(AccountType.PRO_I)
            }
            val accountDetail = mock<AccountDetail> {
                on { this.levelDetail }.thenReturn(levelDetail)
            }

            whenever(accountRepository.hasUserLoggedInBefore()).thenReturn(false)
            whenever(accountRepository.getLoggedInUserId()).thenReturn(userId)
            whenever(
                getSpecificAccountDetailUseCase(
                    storage = false,
                    transfer = false,
                    pro = true
                )
            ).thenReturn(accountDetail)

            val result = underTest.invoke()

            assertThat(result).isFalse()
            verify(accountRepository).addLoggedInUserHandle(userId.id)
        }
}