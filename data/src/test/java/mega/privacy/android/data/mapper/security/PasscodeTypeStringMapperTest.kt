package mega.privacy.android.data.mapper.security

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.passcode.PasscodeType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class PasscodeTypeStringMapperTest {
    private lateinit var underTest: PasscodeTypeStringMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeTypeStringMapper()
    }

    @TestFactory
    fun `test that pin types are mapped to the string of their character count`() =
        (1..10).map {
            DynamicTest.dynamicTest("Test that Pin with count $it returns the string value") {
                assertThat(underTest(PasscodeType.Pin(it))).isEqualTo(it.toString())
            }
        }

    @Test
    fun `test that password type maps to alphanumeric`() {
        assertThat(underTest(PasscodeType.Password)).isEqualTo("alphanumeric")
    }

    @Test
    fun `test that biometric type throws an illegal argument exception`() {
        assertThrows<IllegalArgumentException> { underTest(PasscodeType.Biometric(PasscodeType.Password)) }
    }
}