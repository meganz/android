package mega.privacy.android.core.passcode.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.domain.entity.passcode.PasscodeType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeUITypeToDomainMapperTest {
    private val underTest = PasscodeUITypeToDomainMapper()

    @ParameterizedTest(name = "{0} digits")
    @ValueSource(ints = [4, 6])
    fun `test that a pin maps to a pin with the same number of digits`(digits: Int) {
        assertThat(underTest(PasscodeUIType.Pin(biometricEnabled = false, digits = digits)))
            .isEqualTo(PasscodeType.Pin(digits))
    }

    @Test
    fun `test that an alphanumeric type maps to a password`() {
        assertThat(underTest(PasscodeUIType.Alphanumeric(biometricEnabled = false)))
            .isEqualTo(PasscodeType.Password)
    }

    @Test
    fun `test that a pin maps to a biometric type when biometrics are enabled`() {
        assertThat(underTest(PasscodeUIType.Pin(biometricEnabled = true, digits = 4)))
            .isEqualTo(PasscodeType.Biometric(PasscodeType.Pin(4)))
    }

    @Test
    fun `test that an alphanumeric type maps to a biometric type when biometrics are enabled`() {
        assertThat(underTest(PasscodeUIType.Alphanumeric(biometricEnabled = true)))
            .isEqualTo(PasscodeType.Biometric(PasscodeType.Password))
    }

    @ParameterizedTest(name = "{0} digits")
    @ValueSource(ints = [4, 6])
    fun `test that mapping a pin round trips through PasscodeTypeMapper`(digits: Int) {
        val uiType = PasscodeUIType.Pin(biometricEnabled = true, digits = digits)

        assertThat(PasscodeTypeMapper()(underTest(uiType))).isEqualTo(uiType)
    }
}
