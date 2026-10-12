package mega.privacy.android.domain.usecase.advertisements

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAccountNewUseCaseTest {
    private lateinit var underTest: IsAccountNewUseCase
    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsAccountNewUseCase(
            accountRepository = accountRepository,
        )
    }

    private fun provideIsNewAccountUseCaseParameters() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
    )

    @ParameterizedTest(name = "when accountRepository returns {0} for isAccountNew, then the expected result of IsNewAccountUseCase is {1}")
    @MethodSource("provideIsNewAccountUseCaseParameters")
    fun `test that IsNewAccountUseCase reflects the right values when accountRepository returns them`(
        input: Boolean,
        expected: Boolean,
    ) {
        runTest {
            whenever(accountRepository.isAccountNew()).thenReturn(input)
            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(expected)
        }
    }
}