package mega.privacy.android.app.presentation.settings.passcode.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TimeoutOptionMapperTest {
    private lateinit var underTest: TimeoutOptionMapper

    @BeforeEach
    internal fun setUp() {
        underTest = TimeoutOptionMapper()
    }

    @Test
    fun `test that immediate maps to immediate option`() {
        assertThat(underTest(PasscodeTimeout.Immediate)).isEqualTo(TimeoutOption.Immediate)
    }

    @TestFactory
    fun `test that time span timeouts under 60 seconds are mapped to seconds options`() =
        (1..59).associate { seconds ->
            PasscodeTimeout.TimeSpan(seconds * 1000L) to TimeoutOption.SecondsTimeSpan(seconds)
        }.map { (input, expected) ->
            DynamicTest.dynamicTest("test that timespan timeout of ${input.milliseconds} is mapped to seconds option of ${expected.timeoutInSeconds}") {
                assertThat(underTest(input)).isEqualTo(expected)
            }
        }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 5, 10])
    fun `test that time span timeouts over 60 seconds are mapped to minutes options`(minutes: Int) {
        val milliseconds = minutes * 60 * 1000L
        assertThat(underTest(PasscodeTimeout.TimeSpan(milliseconds))).isEqualTo(
            TimeoutOption.MinutesTimeSpan(
                minutes
            )
        )
    }
}