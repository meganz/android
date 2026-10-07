package mega.privacy.android.feature.chat.managehistory.view

import mega.privacy.android.feature.chat.components.NumberWheel
import kotlin.time.Duration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.divider.SubtleDivider
import mega.privacy.android.feature.chat.R
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Lets the user build a retention period out of an amount and a unit, and apply it.
 *
 * @param state Holds the amount and unit currently on the wheels.
 * @param onConfirm Called with the picked period in seconds.
 * @param modifier [Modifier]
 */
@Composable
internal fun CustomRetentionTimePicker(
    state: CustomRetentionTimePickerState,
    onConfirm: (retentionTime: Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(CUSTOM_RETENTION_TIME_PICKER_TAG)) {
        Row(
            modifier = Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .height(126.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberWheel(
                modifier = Modifier.testTag(RETENTION_AMOUNT_WHEEL_TAG),
                minimumValue = CustomRetentionTimePickerState.MINIMUM_AMOUNT,
                maximumValue = state.maximumAmount,
                currentValue = state.amount,
                onValueChange = state::onAmountChanged,
                onSettled = state::onWheelsSettled,
            )

            Spacer(modifier = Modifier.width(34.dp))

            NumberWheel(
                modifier = Modifier.testTag(RETENTION_UNIT_WHEEL_TAG),
                minimumValue = CustomRetentionTimePickerState.MINIMUM_UNIT,
                maximumValue = CustomRetentionTimePickerState.MAXIMUM_UNIT,
                currentValue = state.unit,
                displayValues = CustomRetentionTimePickerState.UNIT_LABELS.inCurrentLocale(
                    amount = state.amount,
                ),
                onValueChange = state::onUnitChanged,
                onSettled = state::onWheelsSettled,
            )
        }

        TextOnlyButton(
            modifier = Modifier
                .align(Alignment.End)
                .testTag(RETENTION_PICKER_CONFIRM_TAG),
            text = stringResource(sharedR.string.general_ok_only),
            onClick = { onConfirm(state.total()) },
        )

        SubtleDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}


@Composable
private fun List<TimePickerLabel>.inCurrentLocale(amount: Int): List<String> {
    val locale = LocalLocale.current.platformLocale
    return map { label ->
        when (label) {
            is TimePickerLabel.Plural -> pluralStringResource(label.id, amount)
            is TimePickerLabel.Singular -> stringResource(label.id)
        }.lowercase(locale)
    }
}

internal const val CUSTOM_RETENTION_TIME_PICKER_TAG =
    "manage_chat_history_screen:custom_retention_time_picker"
internal const val RETENTION_AMOUNT_WHEEL_TAG =
    "custom_retention_time_picker:amount_wheel"
internal const val RETENTION_UNIT_WHEEL_TAG =
    "custom_retention_time_picker:unit_wheel"
internal const val RETENTION_PICKER_CONFIRM_TAG =
    "custom_retention_time_picker:confirm_button"
