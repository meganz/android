package mega.privacy.android.data.mapper.security

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.passcode.PasscodeType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class PasscodeTypeMapperTest {
    private lateinit var underTest: PasscodeTypeMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeTypeMapper()
    }

    @Test
    internal fun `test that null is returned if string value is null`() {
        assertThat(underTest(null, false)).isNull()
    }

    @TestFactory
    internal fun `test string values`() =
        mapOf(
            "4" to PasscodeType.Pin(4),
            "6" to PasscodeType.Pin(6),
            "alphanumeric" to PasscodeType.Password,
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("test that $expected is returned when $input is passed and biometric is false") {
                assertThat(underTest(input, false)).isEqualTo(expected)
            }
        }

    @TestFactory
    internal fun `test biometric values`() =
        mapOf(
            "4" to PasscodeType.Biometric(PasscodeType.Pin(4)),
            "6" to PasscodeType.Biometric(PasscodeType.Pin(6)),
            "alphanumeric" to PasscodeType.Biometric(PasscodeType.Password),
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("test that $expected is returned when $input is passed and biometric is true") {
                assertThat(underTest(input, true)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that null biometric value is treated as false`() {
        assertThat(underTest("4", null)).isNotInstanceOf(PasscodeType.Biometric::class.java)
    }
}