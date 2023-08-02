package test.mega.privacy.android.app.presentation.passcode.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.passcode.mapper.PasscodeTypeMapper
import mega.privacy.android.app.presentation.passcode.model.PasscodeUIType
import mega.privacy.android.domain.entity.passcode.PasscodeType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class PasscodeTypeMapperTest {
    private lateinit var underTest: PasscodeTypeMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeTypeMapper()
    }

    @TestFactory
    fun `test that non biometric types are mapped`() =
        mapOf(
            PasscodeType.Password to PasscodeUIType.Alphanumeric(false),
            PasscodeType.Pin(4) to PasscodeUIType.Pin(false, 4),
            PasscodeType.Pin(6) to PasscodeUIType.Pin(false, 6),
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("Test that $input is mapped to $expected") {
                assertThat(underTest(input)).isEqualTo(expected)
            }
        }

    @TestFactory
    fun `test that biometric types are mapped to the fallback type`() =
        mapOf(
            PasscodeType.Biometric(PasscodeType.Password) to PasscodeUIType.Alphanumeric(true),
            PasscodeType.Biometric(PasscodeType.Pin(4)) to PasscodeUIType.Pin(true, 4),
            PasscodeType.Biometric(PasscodeType.Pin(6)) to PasscodeUIType.Pin(true, 6),
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("Test that $input is mapped to $expected") {
                assertThat(underTest(input)).isEqualTo(expected)
            }
        }

    @Test
    fun `test that an exception is thrown if a biometric type has a biometric fallback`() {
        assertThrows<IllegalStateException> {
            underTest(
                PasscodeType.Biometric(
                    PasscodeType.Biometric(
                        PasscodeType.Password
                    )
                )
            )
        }
    }
}