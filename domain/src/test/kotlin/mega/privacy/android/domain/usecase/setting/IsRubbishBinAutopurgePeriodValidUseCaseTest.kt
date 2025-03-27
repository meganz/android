package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsRubbishBinAutopurgePeriodValidUseCaseTest {
    private val getAccountTypeUseCase = mock<GetAccountTypeUseCase>()

    private val underTest = IsRubbishBinAutopurgePeriodValidUseCase(getAccountTypeUseCase)

    @Test
    fun `test that when account is paid and days are greater than minimum period returns true`() {
        whenever(getAccountTypeUseCase()).thenReturn(AccountType.PRO_I)
        Truth.assertThat(underTest(7)).isTrue()
    }

    @Test
    fun `test that when account is paid and days are less than or equal to minimum period returns false `() {
        whenever(getAccountTypeUseCase()).thenReturn(AccountType.PRO_I)
        Truth.assertThat(underTest(6)).isFalse()
    }

    @Test
    fun `test that when account is free and days are within valid range returns true`() {
        whenever(getAccountTypeUseCase()).thenReturn(AccountType.FREE)
        Truth.assertThat(underTest(7)).isTrue()
    }

    @Test
    fun `test that when account is free and days are less than or equal to minimum period returns false`() {
        whenever(getAccountTypeUseCase()).thenReturn(AccountType.FREE)
        Truth.assertThat(underTest(6)).isFalse()
    }

    @Test
    fun `test that when account is free and days are greater than maximum period returns false`() {
        whenever(getAccountTypeUseCase()).thenReturn(AccountType.FREE)
        Truth.assertThat(underTest(32)).isFalse()
    }
}