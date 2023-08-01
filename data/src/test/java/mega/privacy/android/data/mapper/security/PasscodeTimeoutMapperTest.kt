package mega.privacy.android.data.mapper.security

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class PasscodeTimeoutMapperTest {
    private lateinit var underTest: PasscodeTimeoutMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeTimeoutMapper()
    }

    @Test
    internal fun `test that null is returned if value is null`() {
        assertThat(underTest(null)).isNull()
    }

    @Test
    internal fun `test that null is returned if value is -1`() {
        assertThat(underTest(-1)).isNull()
    }

    @TestFactory
    internal fun `test that values from 0 to 500 return immediate`() =
        (0L..500L step 25).map {
            DynamicTest.dynamicTest("test that $it returns Immediate timeout") {
                assertThat(underTest(it)).isEqualTo(PasscodeTimeout.Immediate)
            }
        }

    @TestFactory
    internal fun `test that values over 500 returns a timeout of that value`() =
        (501L..1000L step 100).map {
            DynamicTest.dynamicTest("test that $it returns time span timeout") {
                assertThat(underTest(it)).isEqualTo(PasscodeTimeout.TimeSpan(it))
            }
        }
}