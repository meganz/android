package mega.privacy.android.data.mapper.mediaplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RepeatToggleModeMapperTest {
    private val underTest = RepeatToggleModeMapper()

    @TestFactory
    fun `test that repeat toggle mode can be mapped correctly`() =
        listOf(
            RepeatToggleMode.REPEAT_ONE.ordinal to RepeatToggleMode.REPEAT_ONE,
            RepeatToggleMode.REPEAT_NONE.ordinal to RepeatToggleMode.REPEAT_NONE,
            RepeatToggleMode.REPEAT_ALL.ordinal to RepeatToggleMode.REPEAT_ALL,
        ).map { (input, expected) ->
            DynamicTest.dynamicTest("test that $input is mapped to $expected") {
                assertThat(underTest(input)).isEqualTo(expected)
            }
        }
}