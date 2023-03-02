package mega.privacy.android.data.mapper.changepassword

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import nz.mega.sdk.MegaApiJava
import org.junit.Before
import org.junit.Test

class PasswordStrengthMapperImplTest {
    private lateinit var underTest: PasswordStrengthMapper

    @Before
    fun setUp() {
        underTest = PasswordStrengthMapperImpl()
    }

    @Test
    fun `test that mapper return correct value`() {
        assertThat(underTest.invoke(-1)).isEqualTo(PasswordStrength.INVALID)
        assertThat(underTest.invoke(MegaApiJava.PASSWORD_STRENGTH_VERYWEAK)).isEqualTo(
            PasswordStrength.VERY_WEAK
        )
        assertThat(underTest.invoke(MegaApiJava.PASSWORD_STRENGTH_WEAK)).isEqualTo(PasswordStrength.WEAK)
        assertThat(underTest.invoke(MegaApiJava.PASSWORD_STRENGTH_MEDIUM)).isEqualTo(
            PasswordStrength.MEDIUM
        )
        assertThat(underTest.invoke(MegaApiJava.PASSWORD_STRENGTH_GOOD)).isEqualTo(PasswordStrength.GOOD)
        assertThat(underTest.invoke(MegaApiJava.PASSWORD_STRENGTH_STRONG)).isEqualTo(
            PasswordStrength.STRONG
        )
    }
}