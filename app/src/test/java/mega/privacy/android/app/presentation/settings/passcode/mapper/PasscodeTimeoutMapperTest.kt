package mega.privacy.android.app.presentation.settings.passcode.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PasscodeTimeoutMapperTest {
    private lateinit var underTest: PasscodeTimeoutMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeTimeoutMapper()
    }

    @Test
    fun `test that 0L returns immediate timeout`() {
        assertThat(underTest(0L.toString())).isEqualTo(PasscodeTimeout.Immediate)
    }

    @Test
    fun `test that invalid value returns null`() {
        assertThat(underTest("Not a long")).isNull()
    }

    @Test
    fun `test that a value larger than 0 returns time span timeout`() {
        val milliseconds = 1_000L
        assertThat(underTest(milliseconds.toString())).isEqualTo(
            PasscodeTimeout.TimeSpan(
                milliseconds
            )
        )
    }
}