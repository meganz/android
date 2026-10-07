package mega.privacy.android.feature.chat.managehistory.view

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mega.privacy.android.feature.chat.managehistory.model.ONE_MONTH
import mega.privacy.android.feature.chat.managehistory.model.ONE_WEEK
import mega.privacy.android.feature.chat.managehistory.model.ONE_YEAR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Remembers a [CustomRetentionTimePickerState] across configuration changes.
 */
@Composable
internal fun rememberCustomRetentionTimePickerState(): CustomRetentionTimePickerState =
    rememberSaveable(saver = CustomRetentionTimePickerState.Saver) {
        CustomRetentionTimePickerState()
    }

/**
 * Label of a unit on the unit wheel.
 */
internal sealed interface TimePickerLabel {

    /**
     * A label whose wording depends on how many units are picked.
     *
     * @property id Plural resource holding the wording.
     */
    data class Plural(@PluralsRes val id: Int) : TimePickerLabel

    /**
     * A label with a single wording.
     *
     * @property id String resource holding the wording.
     */
    data class Singular(@StringRes val id: Int) : TimePickerLabel
}

/**
 * Drives the two wheels of the custom period picker: how many units, and which unit. The wheels
 * stay consistent with each other, so picking 24 hours rolls over to one day, 30 days or 4 weeks
 * to one month, and 12 months to one year.
 */
@Stable
internal class CustomRetentionTimePickerState {

    var amount: Int by mutableIntStateOf(MINIMUM_AMOUNT)
        private set

    var unit: Int by mutableIntStateOf(UNIT_HOURS)
        private set

    /**
     * Highest amount the unit currently on the wheel allows.
     */
    val maximumAmount: Int
        get() = maximumAmountFor(unit)

    /**
     * Sets the wheels to the period the chat is currently on.
     *
     * @param retentionTime Period the chat currently clears after.
     */
    fun initialiseFrom(retentionTime: Duration) {
        when {
            retentionTime == Duration.ZERO -> {
                unit = UNIT_HOURS
                amount = MINIMUM_AMOUNT
            }

            retentionTime.divisibleBy(ONE_YEAR) -> {
                unit = UNIT_YEARS
                amount = retentionTime.countOf(ONE_YEAR)
            }

            retentionTime.divisibleBy(ONE_MONTH) -> {
                unit = UNIT_MONTHS
                amount = retentionTime.countOf(ONE_MONTH)
            }

            retentionTime.divisibleBy(ONE_WEEK) -> {
                unit = UNIT_WEEKS
                amount = retentionTime.countOf(ONE_WEEK)
            }

            retentionTime.divisibleBy(1.days) -> {
                unit = UNIT_DAYS
                amount = retentionTime.countOf(1.days)
            }

            retentionTime.divisibleBy(1.hours) -> {
                unit = UNIT_HOURS
                amount = retentionTime.countOf(1.hours)
            }

            else -> {
                unit = UNIT_HOURS
                amount = MINIMUM_AMOUNT
            }
        }
    }

    /**
     * Applies a new value on the amount wheel.
     */
    fun onAmountChanged(newValue: Int) {
        amount = newValue
    }

    /**
     * Applies a new value on the unit wheel, capping the amount to what the unit allows.
     */
    fun onUnitChanged(newValue: Int) {
        unit = newValue
        if (amount > maximumAmountFor(newValue)) {
            amount = MINIMUM_AMOUNT
        }
    }

    /**
     * Rolls the picked period over to the next unit up once the wheels come to rest, so that for
     * instance 24 hours is offered back to the user as one day.
     */
    fun onWheelsSettled() {
        val roundedUnit = when {
            amount == MAXIMUM_HOURS && unit == UNIT_HOURS -> UNIT_DAYS

            (amount == DAYS_IN_A_MONTH && unit == UNIT_DAYS) ||
                    (amount == MAXIMUM_WEEKS && unit == UNIT_WEEKS) -> UNIT_MONTHS

            amount == MAXIMUM_MONTHS && unit == UNIT_MONTHS -> UNIT_YEARS

            else -> return
        }
        unit = roundedUnit
        amount = MINIMUM_AMOUNT
    }

    /**
     * The picked period in seconds.
     */
    fun total(): Duration = unitDuration(unit) * amount

    private fun maximumAmountFor(unit: Int): Int = when (unit) {
        UNIT_HOURS -> MAXIMUM_HOURS
        UNIT_DAYS -> MAXIMUM_DAYS
        UNIT_WEEKS -> MAXIMUM_WEEKS
        UNIT_MONTHS -> MAXIMUM_MONTHS
        else -> MINIMUM_AMOUNT
    }

    private fun Duration.divisibleBy(unit: Duration): Boolean =
        inWholeSeconds % unit.inWholeSeconds == 0L

    private fun Duration.countOf(unit: Duration): Int =
        (inWholeSeconds / unit.inWholeSeconds).toInt()

    private fun unitDuration(unit: Int): Duration = when (unit) {
        UNIT_HOURS -> 1.hours
        UNIT_DAYS -> 1.days
        UNIT_WEEKS -> ONE_WEEK
        UNIT_MONTHS -> ONE_MONTH
        else -> ONE_YEAR
    }

    companion object {
        val Saver = listSaver<CustomRetentionTimePickerState, Int>(
            save = { listOf(it.amount, it.unit) },
            restore = { saved ->
                CustomRetentionTimePickerState().apply {
                    amount = saved[0]
                    unit = saved[1]
                }
            },
        )

        /**
         * Labels of the unit wheel, in the order the wheel offers them.
         */
        val UNIT_LABELS = listOf(
            TimePickerLabel.Plural(sharedR.plurals.chat_manage_history_picker_hours),
            TimePickerLabel.Plural(sharedR.plurals.chat_manage_history_picker_days),
            TimePickerLabel.Plural(sharedR.plurals.chat_manage_history_picker_weeks),
            TimePickerLabel.Plural(sharedR.plurals.chat_manage_history_picker_months),
            TimePickerLabel.Singular(sharedR.string.chat_manage_history_picker_year),
        )

        const val MINIMUM_AMOUNT = 1
        const val MINIMUM_UNIT = 0
        const val MAXIMUM_UNIT = 4

        const val UNIT_HOURS = 0
        const val UNIT_DAYS = 1
        const val UNIT_WEEKS = 2
        const val UNIT_MONTHS = 3
        const val UNIT_YEARS = 4

        private const val DAYS_IN_A_MONTH = 30
        private const val MAXIMUM_HOURS = 24
        private const val MAXIMUM_DAYS = 31
        private const val MAXIMUM_WEEKS = 4
        private const val MAXIMUM_MONTHS = 12
    }
}
