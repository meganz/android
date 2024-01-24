package mega.privacy.android.domain.entity

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgressTest {

    @ParameterizedTest
    @ValueSource(floats = [-0.1f, -0.5f, -0.95f, -1f, -110f])
    fun `test that negative values are not allowed`(floatValue: Float) {
        assertThrows<IllegalArgumentException> {
            Progress(floatValue)
        }
    }

    @ParameterizedTest
    @ValueSource(floats = [1.01f, 110f])
    fun `test that values greater than 1 are not allowed`(floatValue: Float) {
        assertThrows<IllegalArgumentException> {
            Progress(floatValue)
        }
    }

    @Test
    @Suppress("DIVISION_BY_ZERO")
    fun `test that NaN value is not allowed`() {
        assertThrows<IllegalArgumentException> {
            Progress(0f / 0f)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [10, 20, 100])
    fun `test that secondary constructor returns correct values`(total: Int) {
        assertThat(Progress(1, total).floatValue).isEqualTo(1f / total.toFloat())
    }

    @ParameterizedTest
    @ValueSource(floats = [0f, 0.1f, 0.5f, 0.95f, 1f])
    fun `test that secondary constructor returns 0 progress when total is 0`(current: Float) {
        assertThat(Progress(current, 0).floatValue).isEqualTo(0f)
    }

    @ParameterizedTest
    @ValueSource(floats = [0f, 0.1f, 0.5f, 0.95f, 1f])
    fun `test that int value is calculated correctly`(floatValue: Float) {
        assertThat(Progress(floatValue).intValue).isEqualTo((floatValue * 100f).toInt())
    }
}