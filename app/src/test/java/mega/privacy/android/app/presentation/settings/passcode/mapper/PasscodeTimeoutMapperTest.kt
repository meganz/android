package mega.privacy.android.app.presentation.settings.passcode.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class PasscodeTimeoutMapperTest {
    private lateinit var underTest: PasscodeTimeoutMapper

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeTimeoutMapper()
    }

    @Test
    fun `test that 0L returns immediate timeout`() {
        assertThat(underTest(TimeoutOption.Immediate)).isEqualTo(PasscodeTimeout.Immediate)
    }

    @Test
    fun `test that a value larger than 0 returns time span timeout`() {
        val milliseconds = 1_000L
        assertThat(
            underTest(
                TimeoutOption.SecondsTimeSpan(
                    milliseconds.milliseconds.inWholeSeconds.toInt()
                )
            )
        ).isEqualTo(
            PasscodeTimeout.TimeSpan(
                milliseconds
            )
        )
    }
}