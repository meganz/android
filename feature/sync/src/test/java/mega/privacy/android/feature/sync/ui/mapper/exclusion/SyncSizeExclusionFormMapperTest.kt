package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncSizeExclusionFormMapperTest {

    private val underTest = SyncSizeExclusionFormMapper()
    private val prefillMapper = SyncSizeExclusionPrefillMapper()

    @Test
    fun `test that invoke declares no bounds when no condition is picked`() {
        val actual = underTest(AddExclusionBySizeFormState(firstBound = bound("100")))

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke declares a smaller bound under smaller than`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.SmallerThan,
                firstBound = bound("2", SyncSizeUnit.Kilobytes),
            ),
        )

        assertThat(actual).isEqualTo(SyncSizeExclusion(excludeSmallerThanBytes = 2 * 1024))
    }

    @Test
    fun `test that invoke declares a larger bound under bigger than`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.BiggerThan,
                firstBound = bound("100", SyncSizeUnit.Megabytes),
            ),
        )

        assertThat(actual).isEqualTo(
            SyncSizeExclusion(excludeLargerThanBytes = 100L * 1024 * 1024),
        )
    }

    @Test
    fun `test that invoke declares both bounds under not between`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = bound("1", SyncSizeUnit.Kilobytes),
                secondBound = bound("2", SyncSizeUnit.Gigabytes),
            ),
        )

        assertThat(actual).isEqualTo(
            SyncSizeExclusion(
                excludeSmallerThanBytes = 1024,
                excludeLargerThanBytes = 2L * 1024 * 1024 * 1024,
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SyncSizeCondition::class)
    fun `test that invoke declares no bounds when the first bound is empty`(
        condition: SyncSizeCondition,
    ) {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = condition,
                secondBound = bound("2", SyncSizeUnit.Gigabytes),
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke declares no bounds when a range is missing its second bound`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = bound("1", SyncSizeUnit.Kilobytes),
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke declares no bounds when a range reads downwards`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = bound("2", SyncSizeUnit.Gigabytes),
                secondBound = bound("1", SyncSizeUnit.Kilobytes),
            ),
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that invoke declares no bounds when a range names the same size twice`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = bound("1024", SyncSizeUnit.Bytes),
                secondBound = bound("1", SyncSizeUnit.Kilobytes),
            ),
        )

        assertThat(actual).isNull()
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["0", "-1", "nine", "99999999999999999999"])
    fun `test that invoke declares no bounds when a bound is not a size a rule can use`(
        value: String,
    ) {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.BiggerThan,
                firstBound = bound(value),
            ),
        )

        assertThat(actual).isNull()
    }

    /**
     * A bound left behind under a condition that no longer reads it must not reach the file, or a
     * user moving from a range to a single limit would keep declaring the bound they abandoned.
     */
    @Test
    fun `test that invoke ignores a second bound the condition does not read`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.BiggerThan,
                firstBound = bound("1", SyncSizeUnit.Kilobytes),
                secondBound = bound("2", SyncSizeUnit.Gigabytes),
            ),
        )

        assertThat(actual).isEqualTo(SyncSizeExclusion(excludeLargerThanBytes = 1024))
    }

    /**
     * A single limit merging into the file's opposite bound is what still reaches the use case as
     * a collision, so the range check must not swallow it here.
     */
    @Test
    fun `test that invoke declares a single bound when the leftover second bound is smaller`() {
        val actual = underTest(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.BiggerThan,
                firstBound = bound("2", SyncSizeUnit.Gigabytes),
                secondBound = bound("1", SyncSizeUnit.Kilobytes),
            ),
        )

        assertThat(actual).isEqualTo(
            SyncSizeExclusion(excludeLargerThanBytes = 2L * 1024 * 1024 * 1024),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SyncSizeCondition::class)
    fun `test that invoke reproduces the bounds a form was prefilled from`(
        condition: SyncSizeCondition,
    ) {
        val original = when (condition) {
            SyncSizeCondition.SmallerThan -> SyncSizeExclusion(excludeSmallerThanBytes = 2048)
            SyncSizeCondition.BiggerThan -> SyncSizeExclusion(excludeLargerThanBytes = 4096)
            SyncSizeCondition.NotBetween -> SyncSizeExclusion(
                excludeSmallerThanBytes = 1024,
                excludeLargerThanBytes = 1024L * 1024,
            )
        }

        val actual = underTest(prefillMapper(original))

        assertThat(actual).isEqualTo(original)
    }

    @Test
    fun `test that invoke declares no bounds for an unbounded file read back through the form`() {
        val actual = underTest(prefillMapper(SyncSizeExclusion()))

        assertThat(actual).isNull()
    }

    private fun bound(value: String, unit: SyncSizeUnit = SyncSizeUnit.Bytes) =
        SyncSizeBoundInput(value = value, unit = unit)
}
