package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.chat.components.reaction.model.UIReaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UIReactionTest {

    @ParameterizedTest(name = "count {0} with {1} digits shows {2}")
    @CsvSource(
        "0, 2, 0",
        "1, 2, 1",
        "99, 2, 99",
        "100, 2, 99+",
        "150, 2, 99+",
        "999, 3, 999",
        "1000, 3, 999+",
    )
    fun `test that countString caps the count at the max digits`(
        count: Int,
        maxDigits: Int,
        expected: String,
    ) {
        val reaction = UIReaction(reaction = "👍", count = count, hasMe = false)

        assertThat(reaction.countString(maxDigits)).isEqualTo(expected)
    }

    @Test
    fun `test that countString uses two digits by default`() {
        val reaction = UIReaction(reaction = "👍", count = 100, hasMe = false)

        assertThat(reaction.countString()).isEqualTo("99+")
    }

    @Test
    fun `test that computeReactionsViewApproximateHeight returns zero when there are no reactions`() {
        assertThat(computeReactionsViewApproximateHeight(0, 360.dp)).isEqualTo(0.dp)
    }

    @Test
    fun `test that computeReactionsViewApproximateHeight returns one row when the reactions fit`() {
        // (360 - 32 - 40) / (44 + 4) = 6 chips per row
        assertThat(computeReactionsViewApproximateHeight(5, 360.dp)).isEqualTo(46.dp)
    }

    @Test
    fun `test that computeReactionsViewApproximateHeight adds a row when the reactions wrap`() {
        assertThat(computeReactionsViewApproximateHeight(6, 360.dp)).isEqualTo(74.dp)
    }

    @Test
    fun `test that computeReactionsViewApproximateHeight uses one chip per row when the screen is narrow`() {
        assertThat(computeReactionsViewApproximateHeight(2, 50.dp)).isEqualTo(102.dp)
    }
}
