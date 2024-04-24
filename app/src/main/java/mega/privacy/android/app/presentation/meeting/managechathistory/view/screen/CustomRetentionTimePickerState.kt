package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.CustomRetentionTimePickerState.Companion.CustomRetentionTimePickerStateSaver
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR

@Composable
internal fun rememberCustomRetentionTimePickerState(): CustomRetentionTimePickerState {
    return rememberSaveable(saver = CustomRetentionTimePickerStateSaver) {
        CustomRetentionTimePickerState()
    }
}

/**
 * State for custom time picker in ManageChatHistoryScreen
 *
 * @property minimumWidth Minimum width of the picker
 * @property minimumValue Minimum value of the picker
 * @property maximumValue Maximum value of the picker
 * @property currentValue Current value of the picker
 * @property displayValues Values to be displayed by the picker
 */
@Immutable
@Parcelize
data class TimePickerItemState(
    val minimumWidth: Int = 0,
    val minimumValue: Int = 0,
    val maximumValue: Int = 0,
    val currentValue: Int = 0,
    val displayValues: List<DisplayValueState>? = null,
) : Parcelable

/**
 * State for the custom time picker's display value
 */
sealed interface DisplayValueState : Parcelable {

    /**
     * Singular string state.
     *
     * @property id The singular string ID for the display value
     */
    @Parcelize
    data class SingularString(@StringRes val id: Int) : DisplayValueState

    /**
     * Plural string state.
     *
     * @property id The plural string ID for the display value
     * @property quantity The quantity corresponds to the plural string
     */
    @Parcelize
    data class PluralString(
        @PluralsRes val id: Int,
        val quantity: Int = 0,
    ) : DisplayValueState
}

@Stable
internal class CustomRetentionTimePickerState {

    internal var ordinalTimePickerItem: TimePickerItemState by mutableStateOf(TimePickerItemState())
        private set

    internal var periodTimePickerItem: TimePickerItemState by mutableStateOf(TimePickerItemState())
        private set

    /**
     * Initialize the time picker property for the UI
     */
    internal fun initializeByRetentionTime(retentionTime: Long) {
        var ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_HOURS
        var ordinalCurrentValue = MINIMUM_VALUE_ORDINAL_PICKER
        var periodCurrentValue = OPTION_HOURS

        when {
            retentionTime == Constants.DISABLED_RETENTION_TIME -> {
                periodCurrentValue = MINIMUM_VALUE_PERIOD_PICKER
            }

            retentionTime.isYearsOnly() -> {
                periodCurrentValue = OPTION_YEARS
                ordinalMaximumValue = MINIMUM_VALUE_ORDINAL_PICKER

                val totalYears = retentionTime / SECONDS_IN_YEAR
                ordinalCurrentValue = totalYears.toInt()
            }

            retentionTime.isMonthsOnly() -> {
                periodCurrentValue = OPTION_MONTHS
                ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS

                val totalMonths = retentionTime / SECONDS_IN_MONTH_30
                ordinalCurrentValue = totalMonths.toInt()
            }

            retentionTime.isWeeksOnly() -> {
                periodCurrentValue = OPTION_WEEKS
                ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS

                val totalWeeks = retentionTime / SECONDS_IN_WEEK
                ordinalCurrentValue = totalWeeks.toInt()
            }

            retentionTime.isDaysOnly() -> {
                periodCurrentValue = OPTION_DAYS
                ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_DAYS

                val totalDays = retentionTime / SECONDS_IN_DAY
                ordinalCurrentValue = totalDays.toInt()
            }

            retentionTime.isHoursOnly() -> {
                val totalHours = retentionTime / SECONDS_IN_HOUR
                ordinalCurrentValue = totalHours.toInt()
            }
        }

        ordinalTimePickerItem = TimePickerItemState(
            minimumValue = MINIMUM_VALUE_ORDINAL_PICKER,
            maximumValue = ordinalMaximumValue,
            currentValue = ordinalCurrentValue
        )
        periodTimePickerItem = TimePickerItemState(
            minimumWidth = MAXIMUM_VALUE_PERIOD_PICKER,
            minimumValue = MINIMUM_VALUE_PERIOD_PICKER,
            maximumValue = MAXIMUM_VALUE_PERIOD_PICKER,
            currentValue = periodCurrentValue,
            displayValues = getDisplayValues(ordinalCurrentValue)
        )
    }

    private fun Long.isYearsOnly() = (this % SECONDS_IN_YEAR) == 0L

    private fun Long.isMonthsOnly() = (this % SECONDS_IN_MONTH_30) == 0L

    private fun Long.isWeeksOnly() = (this % SECONDS_IN_WEEK) == 0L

    private fun Long.isDaysOnly() = (this % SECONDS_IN_DAY) == 0L

    private fun Long.isHoursOnly() = (this % SECONDS_IN_HOUR) == 0L

    /**
     * Update the custom retention time picker value when the ordinal picker value is changed
     *
     * @param oldValue The old value of the ordinal picker
     * @param newValue The new value of the ordinal picker
     */
    internal fun onOrdinalPickerValueChange(oldValue: Int, newValue: Int) {
        val displayedValues = if (shouldUpdateDisplayValues(oldValue, newValue)) {
            getDisplayValues(newValue)
        } else {
            periodTimePickerItem.displayValues
        }

        ordinalTimePickerItem = ordinalTimePickerItem.copy(currentValue = newValue)
        periodTimePickerItem = periodTimePickerItem.copy(displayValues = displayedValues)
    }

    /**
     * Update the custom retention time picker value when the period picker value is changed
     *
     * @param newValue The new value of the period picker
     */
    internal fun onPeriodPickerValueChange(newValue: Int) {
        val maximumValue = getMaximumValueOfNumberPicker(newValue)
        if (ordinalTimePickerItem.currentValue > maximumValue) {
            ordinalTimePickerItem = ordinalTimePickerItem.copy(
                maximumValue = maximumValue,
                currentValue = MINIMUM_VALUE_ORDINAL_PICKER
            )
            periodTimePickerItem = periodTimePickerItem.copy(
                currentValue = newValue,
                displayValues = getDisplayValues(MINIMUM_VALUE_ORDINAL_PICKER)
            )
        } else {
            ordinalTimePickerItem = ordinalTimePickerItem.copy(
                maximumValue = maximumValue
            )
            periodTimePickerItem = periodTimePickerItem.copy(
                currentValue = newValue
            )
        }
    }

    private fun getMaximumValueOfNumberPicker(value: Int): Int =
        when (value) {
            OPTION_HOURS -> MAXIMUM_VALUE_ORDINAL_PICKER_HOURS

            OPTION_DAYS -> MAXIMUM_VALUE_ORDINAL_PICKER_DAYS

            OPTION_WEEKS -> MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS

            OPTION_MONTHS -> MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS

            OPTION_YEARS -> MINIMUM_VALUE_ORDINAL_PICKER

            else -> 0
        }

    private fun shouldUpdateDisplayValues(oldValue: Int, newValue: Int) =
        (oldValue == 1 && newValue > 1) || (newValue == 1 && oldValue > 1)

    /**
     * Get the rounded time picker value when the custom retention time picker
     * is in the idle state after being scrolled
     */
    internal fun onCustomPickerScrollChange() {
        getRoundedTimePickerUiStatesOrNull()?.let { (ordinalPickerItem, periodPickerItem) ->
            ordinalTimePickerItem = ordinalPickerItem
            periodTimePickerItem = periodPickerItem
        }
    }

    /**
     * Method that transforms the chosen option into the most correct form:
     * - If the option selected is 24 hours, it becomes 1 day.
     * - If the selected option is 30 days, it becomes 1 month.
     * - If the selected option is 4 weeks, it becomes 1 month.
     * - If the selected option is 12 months, it becomes 1 year.
     *
     * If none of these criteria are met, then return NULL.
     *
     * @return Pair of the ordinal and the period time picker UI states
     */
    private fun getRoundedTimePickerUiStatesOrNull(): Pair<TimePickerItemState, TimePickerItemState>? =
        when {
            is24HoursPeriod(
                ordinalTimePickerItem.currentValue,
                periodTimePickerItem.currentValue
            ) -> {
                val ordinalPickerUiState = ordinalTimePickerItem.copy(
                    maximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_DAYS,
                    currentValue = MINIMUM_VALUE_ORDINAL_PICKER
                )
                val periodPickerUiState = periodTimePickerItem.copy(
                    currentValue = OPTION_DAYS,
                    displayValues = getDisplayValues(MINIMUM_VALUE_ORDINAL_PICKER)
                )
                Pair(ordinalPickerUiState, periodPickerUiState)
            }

            isOneMonthPeriod(
                ordinalTimePickerItem.currentValue,
                periodTimePickerItem.currentValue
            ) -> {
                val ordinalPickerUiState = ordinalTimePickerItem.copy(
                    maximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS,
                    currentValue = MINIMUM_VALUE_ORDINAL_PICKER
                )
                val periodPickerUiState = periodTimePickerItem.copy(
                    currentValue = OPTION_MONTHS,
                    displayValues = getDisplayValues(MINIMUM_VALUE_ORDINAL_PICKER)
                )
                Pair(ordinalPickerUiState, periodPickerUiState)
            }

            isOneYearPeriod(
                ordinalTimePickerItem.currentValue,
                periodTimePickerItem.currentValue
            ) -> {
                val ordinalPickerUiState = ordinalTimePickerItem.copy(
                    maximumValue = MINIMUM_VALUE_ORDINAL_PICKER,
                    currentValue = MINIMUM_VALUE_ORDINAL_PICKER
                )
                val periodPickerUiState = periodTimePickerItem.copy(
                    currentValue = OPTION_YEARS,
                    displayValues = getDisplayValues(MINIMUM_VALUE_ORDINAL_PICKER)
                )
                Pair(ordinalPickerUiState, periodPickerUiState)
            }

            else -> null
        }

    private fun is24HoursPeriod(ordinalValue: Int, periodValue: Int) =
        ordinalValue == MAXIMUM_VALUE_ORDINAL_PICKER_HOURS && periodValue == OPTION_HOURS

    private fun isOneMonthPeriod(ordinalValue: Int, periodValue: Int) =
        (ordinalValue == DAYS_IN_A_MONTH_VALUE && periodValue == OPTION_DAYS) ||
                (ordinalValue == MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS && periodValue == OPTION_WEEKS)

    private fun isOneYearPeriod(ordinalValue: Int, periodValue: Int) =
        ordinalValue == MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS && periodValue == OPTION_MONTHS

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

    /**
     * Get the total seconds from the current selected time picker's value
     */
    internal fun getTotalSeconds(): Long =
        (getPeriodPickerTotalSeconds() * ordinalTimePickerItem.currentValue).toLong()

    private fun getPeriodPickerTotalSeconds() =
        when (periodTimePickerItem.currentValue) {
            OPTION_HOURS -> SECONDS_IN_HOUR
            OPTION_DAYS -> SECONDS_IN_DAY
            OPTION_WEEKS -> SECONDS_IN_WEEK
            OPTION_MONTHS -> SECONDS_IN_MONTH_30
            else -> SECONDS_IN_YEAR
        }

    companion object {
        val CustomRetentionTimePickerStateSaver: Saver<CustomRetentionTimePickerState, Bundle> =
            run {
                val ordinalTimePicker = "ordinalTimePicker"
                val periodTimePicker = "periodTimePicker"

                Saver(
                    save = {
                        Bundle().apply {
                            putParcelable(ordinalTimePicker, it.ordinalTimePickerItem)
                            putParcelable(periodTimePicker, it.periodTimePickerItem)
                        }
                    },
                    restore = {
                        CustomRetentionTimePickerState().apply {
                            ordinalTimePickerItem = getParcelableItem(
                                bundle = it,
                                key = ordinalTimePicker
                            ) ?: TimePickerItemState()
                            periodTimePickerItem = getParcelableItem(
                                bundle = it,
                                key = periodTimePicker
                            ) ?: TimePickerItemState()
                        }
                    }
                )
            }

        private fun getParcelableItem(bundle: Bundle, key: String): TimePickerItemState? = when {
            SDK_INT >= 33 -> bundle.getParcelable(key, TimePickerItemState::class.java)
            else -> @Suppress("DEPRECATION") bundle.getParcelable(key) as? TimePickerItemState
        }

        private const val DAYS_IN_A_MONTH_VALUE = 30

        private const val OPTION_HOURS = 0
        private const val OPTION_DAYS = 1
        private const val OPTION_MONTHS = 3
        private const val OPTION_WEEKS = 2
        private const val OPTION_YEARS = 4

        private const val MINIMUM_VALUE_ORDINAL_PICKER = 1
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_HOURS = 24
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_DAYS = 31
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS = 4
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS = 12
        private const val MINIMUM_VALUE_PERIOD_PICKER = 0
        private const val MAXIMUM_VALUE_PERIOD_PICKER = 4
    }
}
