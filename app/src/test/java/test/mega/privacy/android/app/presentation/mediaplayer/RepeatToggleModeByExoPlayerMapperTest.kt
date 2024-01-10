package test.mega.privacy.android.app.presentation.mediaplayer

import com.google.android.exoplayer2.Player
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepeatToggleModeByExoPlayerMapperTest {
    private val underTest = RepeatToggleModeByExoPlayerMapper()

    @TestFactory
    fun `test that repeat toggle mode can be mapped correctly`() =
        listOf(
            Player.REPEAT_MODE_ONE to RepeatToggleMode.REPEAT_ONE,
            Player.REPEAT_MODE_OFF to RepeatToggleMode.REPEAT_NONE,
            Player.REPEAT_MODE_ALL to RepeatToggleMode.REPEAT_ALL,
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("test that $input is mapped to $expected") {
                assertThat(underTest(input)).isEqualTo(expected)
            }
        }
}