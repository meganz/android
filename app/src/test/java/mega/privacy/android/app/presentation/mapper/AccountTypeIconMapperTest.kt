package mega.privacy.android.app.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.icon.pack.IconPack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountTypeIconMapperTest {

    private lateinit var underTest: AccountTypeIconMapper

    @BeforeEach
    fun setUp() {
        underTest = AccountTypeIconMapper()
    }

    @Test
    fun `test that PRO_LITE account type returns ShieldLite icon`() {
        val result = underTest(AccountType.PRO_LITE)
        assertThat(result).isEqualTo(IconPack.Medium.Thin.Outline.ShieldLite)
    }

    @Test
    fun `test that PRO_I account type returns Shield01 icon`() {
        val result = underTest(AccountType.PRO_I)
        assertThat(result).isEqualTo(IconPack.Medium.Thin.Outline.Shield01)
    }

    @Test
    fun `test that PRO_II account type returns Shield02 icon`() {
        val result = underTest(AccountType.PRO_II)
        assertThat(result).isEqualTo(IconPack.Medium.Thin.Outline.Shield02)
    }

    @Test
    fun `test that PRO_III account type returns Shield03 icon`() {
        val result = underTest(AccountType.PRO_III)
        assertThat(result).isEqualTo(IconPack.Medium.Thin.Outline.Shield03)
    }

    @ParameterizedTest(name = "Account type {0} returns default Shield icon")
    @MethodSource("provideNonProAccountTypes")
    fun `test that non-PRO account types return default Shield icon`(accountType: AccountType) {
        val result = underTest(accountType)
        assertThat(result).isEqualTo(IconPack.Medium.Thin.Outline.Shield)
    }

    @Test
    fun `test that null account type returns default Shield icon`() {
        val result = underTest(null)
        assertThat(result).isEqualTo(IconPack.Medium.Thin.Outline.Shield)
    }

    private fun provideNonProAccountTypes(): Stream<Arguments> = Stream.of(
        Arguments.of(AccountType.FREE),
        Arguments.of(AccountType.PRO_FLEXI),
        Arguments.of(AccountType.BUSINESS),
        Arguments.of(AccountType.UNKNOWN),
    )
}
