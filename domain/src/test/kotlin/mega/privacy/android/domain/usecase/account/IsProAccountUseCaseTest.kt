package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsProAccountUseCaseTest {

    private val getAccountTypeUseCase: GetAccountTypeUseCase = mock()
    private lateinit var underTest: IsProAccountUseCase

    @BeforeAll
    fun setUp() {
        underTest = IsProAccountUseCase(getAccountTypeUseCase)
    }

    @Test
    fun `test that when account type is free return false`() = runTest {
        whenever(getAccountTypeUseCase()).thenReturn(
            AccountType.FREE
        )

        val result = underTest()

        assertThat(result).isFalse()
    }

    @Test
    fun `test that when account type is pro return true`() = runTest {
        whenever(getAccountTypeUseCase()).thenReturn(
            AccountType.PRO_I
        )

        val result = underTest()

        assertThat(result).isTrue()
    }
}