package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomRetentionTimePickerStateTest {

    private lateinit var underTest: CustomRetentionTimePickerState

    @BeforeEach
    fun setUp() {
        underTest = CustomRetentionTimePickerState()
    }

    @Nested
    @DisplayName("test that the custom picker state is updated correctly")
    inner class OrdinalPickerValueChange {

        @Test
        fun `when the ordinal picker value changes from one (singular) to two (plural)`() {
            val oldValue = 1
            val newValue = 2

            underTest.onOrdinalPickerValueChange(oldValue = oldValue, newValue = newValue)

            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(newValue)
            assertThat(
                underTest.periodTimePickerItem.displayValues
            ).isEqualTo(getDisplayValues(newValue))
        }

        @Test
        fun `when the ordinal picker value changes from two (plural) to one (singular)`() {
            val oldValue = 2
            val newValue = 1

            underTest.onOrdinalPickerValueChange(oldValue = oldValue, newValue = newValue)

            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(newValue)
            assertThat(
                underTest.periodTimePickerItem.displayValues
            ).isEqualTo(getDisplayValues(newValue))
        }

        @Test
        fun `when the ordinal picker value changes from two (plural) to three (plural)`() {
            val oldValue = 2
            val newValue = 3

            underTest.onOrdinalPickerValueChange(oldValue = oldValue, newValue = newValue)

            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(newValue)
            assertThat(
                underTest.periodTimePickerItem.displayValues
            ).isEqualTo(null) // null indicates that the value is not changed
        }
    }

    @Nested
    @DisplayName("test that the custom picker state is updated correctly")
    inner class PeriodPickerValueChange {

        @Test
        fun `when selected ordinal value is more than maximum value for hours`() {
            val periodNewValue = 0 // OPTION_HOURS
            val ordinalMaximumValue = 24 // MAXIMUM_VALUE_ORDINAL_PICKER_HOURS
            val ordinalCurrentValue = 31 // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalCurrentValue)

            underTest.onPeriodPickerValueChange(periodNewValue)

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(ordinalMaximumValue)
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1) // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(periodNewValue)
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1)) // MINIMUM_VALUE_ORDINAL_PICKER
        }

        @Test
        fun `when selected ordinal value is more than maximum value for weeks`() {
            val periodNewValue = 2 // OPTION_WEEKS
            val ordinalMaximumValue = 4 // MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS
            val ordinalCurrentValue = 31 // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalCurrentValue)

            underTest.onPeriodPickerValueChange(periodNewValue)

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(ordinalMaximumValue)
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1) // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(periodNewValue)
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1)) // MINIMUM_VALUE_ORDINAL_PICKER
        }

        @Test
        fun `when selected ordinal value is more than maximum value for months`() {
            val periodNewValue = 3 // OPTION_MONTHS
            val ordinalMaximumValue = 12 // MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS
            val ordinalCurrentValue = 31 // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalCurrentValue)

            underTest.onPeriodPickerValueChange(periodNewValue)

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(ordinalMaximumValue)
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1) // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(periodNewValue)
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1)) // MINIMUM_VALUE_ORDINAL_PICKER
        }

        @Test
        fun `when selected ordinal value is more than maximum value for years`() {
            val periodNewValue = 4 // OPTION_YEARS
            val ordinalMaximumValue = 1 // MINIMUM_VALUE_ORDINAL_PICKER
            val ordinalCurrentValue = 31 // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalCurrentValue)

            underTest.onPeriodPickerValueChange(periodNewValue)

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(ordinalMaximumValue)
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1) // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(periodNewValue)
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1)) // MINIMUM_VALUE_ORDINAL_PICKER
        }

        @Test
        fun `when selected ordinal value is not greater than maximum value`() {
            val periodNewValue = 1 // OPTION_DAYS
            val ordinalMaximumValue = 31 // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            val ordinalCurrentValue = 31 // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalCurrentValue)

            underTest.onPeriodPickerValueChange(periodNewValue)

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(ordinalMaximumValue)
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(ordinalCurrentValue)
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(periodNewValue)
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(null) // null indicates that the display values are not updated
        }
    }

    @Nested
    @DisplayName("test that the custom picker state is updated correctly")
    inner class ScrollChange {

        @Test
        fun `when the scroll state is idle at twenty four hours period`() {
            val ordinalValue = 24
            val periodValue = 0 // OPTION_HOURS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalValue)
            underTest.onPeriodPickerValueChange(periodValue)

            underTest.onCustomPickerScrollChange()

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(31) // MAXIMUM_VALUE_ORDINAL_PICKER_DAYS
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1)  // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(1)  // OPTION_DAYS
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1))
        }

        @Test
        fun `when the scroll state is idle at thirty days should become one month period`() {
            val ordinalValue = 30
            val periodValue = 1 // OPTION_DAYS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalValue)
            underTest.onPeriodPickerValueChange(periodValue)

            underTest.onCustomPickerScrollChange()

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(12) // MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1)  // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(3)  // OPTION_MONTHS
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1))
        }

        @Test
        fun `when the scroll state is idle at four weeks should become one month period`() {
            val ordinalValue = 4
            val periodValue = 2 // OPTION_WEEKS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalValue)
            underTest.onPeriodPickerValueChange(periodValue)

            underTest.onCustomPickerScrollChange()

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(12) // MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1)  // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(3)  // OPTION_MONTHS
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1))
        }

        @Test
        fun `when the scroll state is idle at one year period`() {
            val ordinalValue = 12
            val periodValue = 3 // OPTION_MONTHS
            underTest.onOrdinalPickerValueChange(oldValue = 0, newValue = ordinalValue)
            underTest.onPeriodPickerValueChange(periodValue)

            underTest.onCustomPickerScrollChange()

            assertThat(underTest.ordinalTimePickerItem.maximumValue).isEqualTo(1) // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.ordinalTimePickerItem.currentValue).isEqualTo(1)  // MINIMUM_VALUE_ORDINAL_PICKER
            assertThat(underTest.periodTimePickerItem.currentValue).isEqualTo(4)  // OPTION_YEARS
            assertThat(underTest.periodTimePickerItem.displayValues).isEqualTo(getDisplayValues(1))
        }
    }

    private fun getDisplayValues(value: Int) = listOf(
        DisplayValueState.PluralString(
            id = R.plurals.retention_time_picker_hours,
            quantity = value
        ),
        DisplayValueState.PluralString(
            id = R.plurals.retention_time_picker_days,
            quantity = value
        ),
        DisplayValueState.PluralString(
            id = R.plurals.retention_time_picker_weeks,
            quantity = value
        ),
        DisplayValueState.PluralString(
            id = R.plurals.retention_time_picker_months,
            quantity = value
        ),
        DisplayValueState.SingularString(
            id = R.string.retention_time_picker_year
        )
    )

    @Nested
    @DisplayName("test that the custom time picker is initialized with correct items")
    inner class InitializePickerData {

        @Test
        fun `when the current retention time is disabled`() {
            val retentionTime = DISABLED_RETENTION_TIME

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 24,
                currentValue = 1
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 0,
                displayValues = getDisplayValues(1)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }

        @Test
        fun `when the current retention time is in years only period`() {
            val retentionTime = SECONDS_IN_YEAR.toLong()

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 1,
                currentValue = 1
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 4,
                displayValues = getDisplayValues(1)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }

        @Test
        fun `when the current retention time is in months only period`() {
            val retentionTime = SECONDS_IN_MONTH_30.toLong() * 12

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 12,
                currentValue = 12
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 3,
                displayValues = getDisplayValues(12)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }

        @Test
        fun `when the current retention time is in weeks only period`() {
            val retentionTime = SECONDS_IN_WEEK.toLong() * 4

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 4,
                currentValue = 4
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 2,
                displayValues = getDisplayValues(4)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }

        @Test
        fun `when the current retention time is in days only period`() {
            val retentionTime = SECONDS_IN_DAY.toLong() * 31

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 31,
                currentValue = 31
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 1,
                displayValues = getDisplayValues(31)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }

        @Test
        fun `when the current retention time is in hours only period`() {
            val retentionTime = SECONDS_IN_HOUR.toLong() * 23

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 24,
                currentValue = 23
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 0,
                displayValues = getDisplayValues(23)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }

        @Test
        fun `when the current retention time is less than one hour period`() {
            val retentionTime = 100L

            underTest.initializeByRetentionTime(retentionTime)

            val expectedOrdinalState = TimePickerItemState(
                minimumValue = 1,
                maximumValue = 24,
                currentValue = 1
            )
            val expectedPeriodState = TimePickerItemState(
                minimumWidth = 4,
                minimumValue = 0,
                maximumValue = 4,
                currentValue = 0,
                displayValues = getDisplayValues(1)
            )
            assertThat(underTest.ordinalTimePickerItem).isEqualTo(expectedOrdinalState)
            assertThat(underTest.periodTimePickerItem).isEqualTo(expectedPeriodState)
        }
    }
}
