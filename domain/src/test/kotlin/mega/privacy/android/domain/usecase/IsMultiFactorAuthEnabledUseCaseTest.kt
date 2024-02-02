package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsMultiFactorAuthEnabledUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsMultiFactorAuthEnabledUseCaseTest {
    private lateinit var underTest: IsMultiFactorAuthEnabledUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsMultiFactorAuthEnabledUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @ParameterizedTest(name = "is multi-factor authentication enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the multi-factor authentication state is returned`(
        isMultiFactorAuthenticationEnabled: Boolean,
    ) = runTest {
        whenever(accountRepository.isMultiFactorAuthEnabled()).thenReturn(
            isMultiFactorAuthenticationEnabled
        )
        assertThat(underTest()).isEqualTo(isMultiFactorAuthenticationEnabled)
    }

    @Test
    fun `test that an exception is thrown when an issue occurs in retrieving the multi-factor authentication state`() =
        runTest {
            whenever(accountRepository.isMultiFactorAuthEnabled()).thenThrow(RuntimeException())
            assertThrows<RuntimeException> { underTest() }
        }
}