package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.user.UserId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [ShouldShowUpgradeAccountUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShouldShowUpgradeAccountUseCaseTest {
    private lateinit var underTest: ShouldShowUpgradeAccountUseCase

    private val getSpecificAccountDetailUseCase = mock<GetSpecificAccountDetailUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = ShouldShowUpgradeAccountUseCase(
            getSpecificAccountDetailUseCase = getSpecificAccountDetailUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset( getSpecificAccountDetailUseCase)
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

            whenever(
                getSpecificAccountDetailUseCase(
                    storage = false,
                    transfer = false,
                    pro = true
                )
            ).thenReturn(accountDetail)

            val result = underTest.invoke()

            assertThat(result).isTrue()
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

            whenever(
                getSpecificAccountDetailUseCase(
                    storage = false,
                    transfer = false,
                    pro = true
                )
            ).thenReturn(accountDetail)

            val result = underTest.invoke()

            assertThat(result).isFalse()
        }
}