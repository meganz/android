package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsWeakPasswordUseCaseTest {

    private lateinit var underTest: IsWeakPasswordUseCase
    private val getPasswordStrengthUseCase: GetPasswordStrengthUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = IsWeakPasswordUseCase(getPasswordStrengthUseCase)
    }

    @Test
    fun `test that invoke returns true for null password`() = runBlocking {
        val result = underTest.invoke(null)
        assertThat(result).isTrue()
    }

    @Test
    fun `test that invoke returns true for blank password`() = runBlocking {
        val result = underTest.invoke("")
        assertThat(result).isTrue()
    }

    @Test
    fun `test that invoke returns true for invalid password strength`() = runBlocking {
        whenever(getPasswordStrengthUseCase("password")).thenReturn(PasswordStrength.INVALID)
        val result = underTest.invoke("password")
        assertThat(result).isTrue()
    }

    @Test
    fun `test that invoke returns true for very weak password strength`() = runBlocking {
        whenever(getPasswordStrengthUseCase("password")).thenReturn(PasswordStrength.VERY_WEAK)
        val result = underTest.invoke("password")
        assertThat(result).isTrue()
    }

    @Test
    fun `test that invoke returns true for weak password strength`() = runBlocking {
        whenever(getPasswordStrengthUseCase("password")).thenReturn(PasswordStrength.WEAK)
        val result = underTest.invoke("password")
        assertThat(result).isTrue()
    }

    @Test
    fun `test that invoke returns false for medium password strength`() = runBlocking {
        whenever(getPasswordStrengthUseCase("password")).thenReturn(PasswordStrength.MEDIUM)
        val result = underTest.invoke("password")
        assertThat(result).isFalse()
    }

    @Test
    fun `test that invoke returns false for good password strength`() = runBlocking {
        whenever(getPasswordStrengthUseCase("password")).thenReturn(PasswordStrength.GOOD)
        val result = underTest.invoke("password")
        assertThat(result).isFalse()
    }

    @Test
    fun `test that invoke returns false for strong password strength`() = runBlocking {
        whenever(getPasswordStrengthUseCase("password")).thenReturn(PasswordStrength.STRONG)
        val result = underTest.invoke("password")
        assertThat(result).isFalse()
    }
}