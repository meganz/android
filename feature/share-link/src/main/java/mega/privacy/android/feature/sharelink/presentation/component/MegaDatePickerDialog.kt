package mega.privacy.android.feature.sharelink.presentation.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R as sharedR
import java.util.Calendar
import java.util.TimeZone

/**
 * Reusable Material3 date-picker dialog for the revamped Share link feature.
 *
 * The design system has no date picker, so this wraps Material3's [DatePickerDialog] with MEGA's
 * text buttons and restricts the selection to today onwards (a link expiry cannot be in the past),
 * mirroring the legacy `minDate` behaviour.
 *
 * @param onDateSelected Invoked with the selected date, in UTC milliseconds, when the user confirms.
 * @param onDismiss Invoked when the user cancels or dismisses the dialog.
 * @param modifier Modifier for the dialog.
 * @param initialSelectedTimeMillis The date to pre-select, in UTC milliseconds, or null for none.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaDatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedTimeMillis: Long? = null,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedTimeMillis,
        selectableDates = TodayOnwardSelectableDates,
    )
    val confirmEnabled by remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }
    val colors = megaDatePickerColors()

    DatePickerDialog(
        modifier = modifier.testTag(MEGA_DATE_PICKER_DIALOG_TAG),
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextOnlyButton(
                modifier = Modifier.testTag(MEGA_DATE_PICKER_CONFIRM_TAG),
                text = stringResource(sharedR.string.general_ok_only),
                enabled = confirmEnabled,
                onClick = { datePickerState.selectedDateMillis?.let(onDateSelected) },
            )
        },
        dismissButton = {
            TextOnlyButton(
                modifier = Modifier.testTag(MEGA_DATE_PICKER_DISMISS_TAG),
                text = stringResource(sharedR.string.general_dialog_cancel_button),
                onClick = onDismiss,
            )
        },
    ) {
        DatePicker(state = datePickerState, colors = colors)
    }
}

/**
 * Maps the Material3 date-picker palette onto MEGA design tokens.
 *
 * [mega.android.core.ui.theme.AndroidTheme] does not populate the Material3 `colorScheme`, so the
 * platform picker would otherwise fall back to the stock baseline colors and ignore dark mode.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun megaDatePickerColors(): DatePickerColors = DatePickerDefaults.colors(
    containerColor = DSTokens.colors.background.surface1,
    titleContentColor = DSTokens.colors.text.secondary,
    headlineContentColor = DSTokens.colors.text.primary,
    weekdayContentColor = DSTokens.colors.text.secondary,
    subheadContentColor = DSTokens.colors.text.secondary,
    navigationContentColor = DSTokens.colors.icon.primary,
    yearContentColor = DSTokens.colors.text.primary,
    disabledYearContentColor = DSTokens.colors.text.disabled,
    currentYearContentColor = DSTokens.colors.text.accent,
    selectedYearContentColor = DSTokens.colors.text.onColor,
    disabledSelectedYearContentColor = DSTokens.colors.text.onColorDisabled,
    selectedYearContainerColor = DSTokens.colors.button.brand,
    disabledSelectedYearContainerColor = DSTokens.colors.button.disabled,
    dayContentColor = DSTokens.colors.text.primary,
    disabledDayContentColor = DSTokens.colors.text.disabled,
    selectedDayContentColor = DSTokens.colors.text.onColor,
    disabledSelectedDayContentColor = DSTokens.colors.text.onColorDisabled,
    selectedDayContainerColor = DSTokens.colors.button.brand,
    disabledSelectedDayContainerColor = DSTokens.colors.button.disabled,
    todayContentColor = DSTokens.colors.text.accent,
    todayDateBorderColor = DSTokens.colors.border.strongSelected,
    dayInSelectionRangeContentColor = DSTokens.colors.text.primary,
    dayInSelectionRangeContainerColor = DSTokens.colors.background.surface2,
    dividerColor = DSTokens.colors.border.subtle,
)

@OptIn(ExperimentalMaterial3Api::class)
private object TodayOnwardSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= todayStartUtcMillis()

    override fun isSelectableYear(year: Int): Boolean =
        year >= Calendar.getInstance(UTC).get(Calendar.YEAR)
}

private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

private fun todayStartUtcMillis(): Long =
    Calendar.getInstance(UTC).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun MegaDatePickerContentPreview() {
    AndroidThemeForPreviews {
        DatePicker(
            state = rememberDatePickerState(selectableDates = TodayOnwardSelectableDates),
            colors = megaDatePickerColors(),
        )
    }
}

internal const val MEGA_DATE_PICKER_DIALOG_TAG = "mega_date_picker_dialog:dialog"
internal const val MEGA_DATE_PICKER_CONFIRM_TAG = "mega_date_picker_dialog:button_confirm"
internal const val MEGA_DATE_PICKER_DISMISS_TAG = "mega_date_picker_dialog:button_dismiss"
