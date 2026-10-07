package mega.privacy.android.feature.chat.managehistory.view

import kotlin.time.Duration.Companion.seconds
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class CustomRetentionTimePickerStateTest {

    private val underTest = CustomRetentionTimePickerState()

    @ParameterizedTest(name = "{0} seconds is {1} of unit {2}")
    @MethodSource("retentionTimes")
    fun `test that initialiseFrom puts the wheels on the period`(
        retentionTime: Long,
        expectedAmount: Int,
        expectedUnit: Int,
    ) {
        underTest.initialiseFrom(retentionTime.seconds)

        assertThat(underTest.amount).isEqualTo(expectedAmount)
        assertThat(underTest.unit).isEqualTo(expectedUnit)
    }

    @ParameterizedTest(name = "{0} of unit {1} is {2} seconds")
    @MethodSource("pickedPeriods")
    fun `test that total returns the picked period`(
        amount: Int,
        unit: Int,
        expected: Long,
    ) {
        underTest.onUnitChanged(unit)
        underTest.onAmountChanged(amount)

        assertThat(underTest.total()).isEqualTo(expected.seconds)
    }

    @Test
    fun `test that onUnitChanged resets the amount when the unit cannot hold it`() {
        underTest.onUnitChanged(CustomRetentionTimePickerState.UNIT_HOURS)
        underTest.onAmountChanged(20)

        underTest.onUnitChanged(CustomRetentionTimePickerState.UNIT_WEEKS)

        assertThat(underTest.amount).isEqualTo(CustomRetentionTimePickerState.MINIMUM_AMOUNT)
        assertThat(underTest.maximumAmount).isEqualTo(4)
    }

    @Test
    fun `test that onUnitChanged keeps the amount when the unit can hold it`() {
        underTest.onUnitChanged(CustomRetentionTimePickerState.UNIT_HOURS)
        underTest.onAmountChanged(3)

        underTest.onUnitChanged(CustomRetentionTimePickerState.UNIT_DAYS)

        assertThat(underTest.amount).isEqualTo(3)
    }

    @ParameterizedTest(name = "{0} of unit {1} rounds to 1 of unit {2}")
    @MethodSource("roundedPeriods")
    fun `test that onWheelsSettled rolls the period over to the next unit up`(
        amount: Int,
        unit: Int,
        expectedUnit: Int,
    ) {
        underTest.onUnitChanged(unit)
        underTest.onAmountChanged(amount)

        underTest.onWheelsSettled()

        assertThat(underTest.amount).isEqualTo(CustomRetentionTimePickerState.MINIMUM_AMOUNT)
        assertThat(underTest.unit).isEqualTo(expectedUnit)
    }

    @Test
    fun `test that onWheelsSettled leaves a period that does not roll over alone`() {
        underTest.onUnitChanged(CustomRetentionTimePickerState.UNIT_DAYS)
        underTest.onAmountChanged(3)

        underTest.onWheelsSettled()

        assertThat(underTest.amount).isEqualTo(3)
        assertThat(underTest.unit).isEqualTo(CustomRetentionTimePickerState.UNIT_DAYS)
    }

    companion object {

        @JvmStatic
        fun retentionTimes(): Stream<Arguments> = Stream.of(
            Arguments.of(0L, 1, CustomRetentionTimePickerState.UNIT_HOURS),
            Arguments.of(3_600L, 1, CustomRetentionTimePickerState.UNIT_HOURS),
            Arguments.of(18_000L, 5, CustomRetentionTimePickerState.UNIT_HOURS),
            Arguments.of(86_400L, 1, CustomRetentionTimePickerState.UNIT_DAYS),
            Arguments.of(604_800L, 1, CustomRetentionTimePickerState.UNIT_WEEKS),
            Arguments.of(2_592_000L, 1, CustomRetentionTimePickerState.UNIT_MONTHS),
            Arguments.of(31_536_000L, 1, CustomRetentionTimePickerState.UNIT_YEARS),
            Arguments.of(100L, 1, CustomRetentionTimePickerState.UNIT_HOURS),
        )

        @JvmStatic
        fun pickedPeriods(): Stream<Arguments> = Stream.of(
            Arguments.of(5, CustomRetentionTimePickerState.UNIT_HOURS, 18_000L),
            Arguments.of(3, CustomRetentionTimePickerState.UNIT_DAYS, 259_200L),
            Arguments.of(2, CustomRetentionTimePickerState.UNIT_WEEKS, 1_209_600L),
            Arguments.of(6, CustomRetentionTimePickerState.UNIT_MONTHS, 15_552_000L),
            Arguments.of(1, CustomRetentionTimePickerState.UNIT_YEARS, 31_536_000L),
        )

        @JvmStatic
        fun roundedPeriods(): Stream<Arguments> = Stream.of(
            Arguments.of(
                24,
                CustomRetentionTimePickerState.UNIT_HOURS,
                CustomRetentionTimePickerState.UNIT_DAYS,
            ),
            Arguments.of(
                30,
                CustomRetentionTimePickerState.UNIT_DAYS,
                CustomRetentionTimePickerState.UNIT_MONTHS,
            ),
            Arguments.of(
                4,
                CustomRetentionTimePickerState.UNIT_WEEKS,
                CustomRetentionTimePickerState.UNIT_MONTHS,
            ),
            Arguments.of(
                12,
                CustomRetentionTimePickerState.UNIT_MONTHS,
                CustomRetentionTimePickerState.UNIT_YEARS,
            ),
        )
    }
}
