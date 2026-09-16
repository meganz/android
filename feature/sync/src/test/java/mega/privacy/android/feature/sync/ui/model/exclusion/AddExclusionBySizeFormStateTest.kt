package mega.privacy.android.feature.sync.ui.model.exclusion

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

internal class AddExclusionBySizeFormStateTest {

    @Test
    fun `test that showsBounds hides the fields when no condition is picked`() {
        val underTest = AddExclusionBySizeFormState()

        assertThat(underTest.showsBounds).isFalse()
        assertThat(underTest.activeBounds).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(SyncSizeCondition::class)
    fun `test that showsBounds shows the fields when a condition is picked`(
        condition: SyncSizeCondition,
    ) {
        val underTest = AddExclusionBySizeFormState(condition = condition)

        assertThat(underTest.showsBounds).isTrue()
    }

    @ParameterizedTest
    @EnumSource(
        value = SyncSizeCondition::class,
        names = ["BiggerThan", "SmallerThan"],
    )
    fun `test that showsSecondBound hides the second field for a single bound condition`(
        condition: SyncSizeCondition,
    ) {
        val underTest = AddExclusionBySizeFormState(condition = condition)

        assertThat(underTest.showsSecondBound).isFalse()
        assertThat(underTest.activeBounds).hasSize(1)
    }

    @Test
    fun `test that showsSecondBound shows the second field when the condition is a range`() {
        val underTest = AddExclusionBySizeFormState(condition = SyncSizeCondition.NotBetween)

        assertThat(underTest.showsSecondBound).isTrue()
        assertThat(underTest.activeBounds).hasSize(2)
    }

    @Test
    fun `test that activeBounds lists a range in the order the bounds are drawn`() {
        val first = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes)
        val second = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes)
        val underTest = AddExclusionBySizeFormState(
            condition = SyncSizeCondition.NotBetween,
            firstBound = first,
            secondBound = second,
        )

        assertThat(underTest.activeBounds).containsExactly(first, second).inOrder()
    }

    @Test
    fun `test that activeBounds ignores the second bound of a single bound condition`() {
        val underTest = AddExclusionBySizeFormState(
            condition = SyncSizeCondition.BiggerThan,
            firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Gigabytes),
            secondBound = SyncSizeBoundInput(value = "9", unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.activeBounds)
            .containsExactly(SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Gigabytes))
    }

    @Test
    fun `test that each bound of a range keeps its own unit`() {
        val underTest = AddExclusionBySizeFormState(
            condition = SyncSizeCondition.NotBetween,
            firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
            secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
        )

        assertThat(underTest.firstBound.bytes).isEqualTo(1_024L)
        assertThat(underTest.secondBound.bytes).isEqualTo(2_147_483_648L)
    }

    @Test
    fun `test that bytes reads a bound in the unit beside it`() {
        val underTest = SyncSizeBoundInput(value = "100", unit = SyncSizeUnit.Megabytes)

        assertThat(underTest.bytes).isEqualTo(104_857_600L)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "0", "-1", "1.5", "1 MB", "abc"])
    fun `test that bytes refuses a bound that is not a positive whole number`(value: String) {
        val underTest = SyncSizeBoundInput(value = value)

        assertThat(underTest.bytes).isNull()
    }

    @Test
    fun `test that bytes returns null when the bound overflows rather than wrapping negative`() {
        val underTest = SyncSizeBoundInput(
            value = "${Long.MAX_VALUE}",
            unit = SyncSizeUnit.Gigabytes,
        )

        assertThat(underTest.bytes).isNull()
    }

    @Test
    fun `test that bytes returns null when the bound has more digits than a Long holds`() {
        val underTest = SyncSizeBoundInput(value = "9".repeat(30), unit = SyncSizeUnit.Bytes)

        assertThat(underTest.bytes).isNull()
    }

    @Test
    fun `test that showsRangeError stays quiet before a condition is picked`() {
        assertThat(AddExclusionBySizeFormState().showsRangeError).isFalse()
    }

    @ParameterizedTest
    @EnumSource(
        value = SyncSizeCondition::class,
        names = ["BiggerThan", "SmallerThan"],
    )
    fun `test that showsRangeError ignores the second bound of a single bound condition`(
        condition: SyncSizeCondition,
    ) {
        val underTest = AddExclusionBySizeFormState(
            condition = condition,
            firstBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            secondBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.showsRangeError).isFalse()
    }

    @Test
    fun `test that showsRangeError accepts a range whose first bound is the smaller one`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
            second = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
        )

        assertThat(underTest.showsRangeError).isFalse()
    }

    @Test
    fun `test that showsRangeError rejects a range typed the wrong way round`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            second = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
        )

        assertThat(underTest.showsRangeError).isTrue()
    }

    @Test
    fun `test that showsRangeError compares the bounds in bytes rather than as typed`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Gigabytes),
            second = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Kilobytes),
        )

        assertThat(underTest.showsRangeError).isTrue()
    }

    @Test
    fun `test that showsRangeError rejects a range whose bounds are the same size`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "1024", unit = SyncSizeUnit.Bytes),
            second = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
        )

        assertThat(underTest.showsRangeError).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "abc"])
    fun `test that showsRangeError waits until the second bound is a usable size`(value: String) {
        val underTest = range(
            first = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            second = SyncSizeBoundInput(value = value, unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.showsRangeError).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "abc"])
    fun `test that showsRangeError waits until the first bound is a usable size`(value: String) {
        val underTest = range(
            first = SyncSizeBoundInput(value = value, unit = SyncSizeUnit.Gigabytes),
            second = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.showsRangeError).isFalse()
    }

    @Test
    fun `test that showsRangeError stays quiet when a bound is too large to hold`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "${Long.MAX_VALUE}", unit = SyncSizeUnit.Gigabytes),
            second = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.showsRangeError).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "abc", "9999999999"])
    fun `test that hasUnusableBound reports a bound typed as something that is not a size`(
        value: String,
    ) {
        val underTest = AddExclusionBySizeFormState(
            condition = SyncSizeCondition.BiggerThan,
            firstBound = SyncSizeBoundInput(value = value, unit = SyncSizeUnit.Gigabytes),
        )

        assertThat(underTest.hasUnusableBound).isTrue()
    }

    @Test
    fun `test that hasUnusableBound ignores a bound that has not been typed yet`() {
        val underTest = AddExclusionBySizeFormState(condition = SyncSizeCondition.NotBetween)

        assertThat(underTest.hasUnusableBound).isFalse()
    }

    @Test
    fun `test that hasUnusableBound ignores the second bound of a single bound condition`() {
        val underTest = AddExclusionBySizeFormState(
            condition = SyncSizeCondition.BiggerThan,
            firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Gigabytes),
            secondBound = SyncSizeBoundInput(value = "0", unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.hasUnusableBound).isFalse()
    }

    @Test
    fun `test that a bound too large to hold is unusable rather than out of order`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Bytes),
            second = SyncSizeBoundInput(value = "9999999999", unit = SyncSizeUnit.Gigabytes),
        )

        assertThat(underTest.hasUnusableBound).isTrue()
        assertThat(underTest.showsRangeError).isFalse()
    }

    @Test
    fun `test that a zero bound is unusable rather than out of order`() {
        val underTest = range(
            first = SyncSizeBoundInput(value = "5", unit = SyncSizeUnit.Gigabytes),
            second = SyncSizeBoundInput(value = "0", unit = SyncSizeUnit.Bytes),
        )

        assertThat(underTest.hasUnusableBound).isTrue()
        assertThat(underTest.showsRangeError).isFalse()
    }

    private fun range(first: SyncSizeBoundInput, second: SyncSizeBoundInput) =
        AddExclusionBySizeFormState(
            condition = SyncSizeCondition.NotBetween,
            firstBound = first,
            secondBound = second,
        )

    @Test
    fun `test that isEmpty reports an untouched bound`() {
        assertThat(SyncSizeBoundInput().isEmpty).isTrue()
        assertThat(SyncSizeBoundInput(value = "1").isEmpty).isFalse()
    }
}
