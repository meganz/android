package mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] to display the chat history retention time options in a dialog
 *
 * @param currentRetentionTime The current retention time for a specific chat room
 * @param onDismissRequest The callback that will be triggered when the user requests to dismiss the dialog
 * @param onConfirmClick The callback that will be triggered when the user clicks the confirm button (either the 'Next' or 'OK' button)
 * @param modifier The [Modifier] for this [Composable]
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChatHistoryRetentionConfirmationDialog(
    currentRetentionTime: Long,
    onDismissRequest: () -> Unit,
    onConfirmClick: (option: ChatHistoryRetentionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by rememberSaveable {
        mutableStateOf(getOptionFromRetentionTime(currentRetentionTime))
    }
    val isCustomOptionSelected by remember {
        derivedStateOf {
            selectedOption == ChatHistoryRetentionOption.Custom
        }
    }
    val isConfirmButtonEnable by remember(currentRetentionTime) {
        derivedStateOf {
            getOptionFromRetentionTime(currentRetentionTime) != ChatHistoryRetentionOption.Disabled ||
                    selectedOption != ChatHistoryRetentionOption.Disabled
        }
    }

    ConfirmationDialogWithRadioButtons(
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG),
        titleText = stringResource(id = R.string.title_properties_history_retention),
        subTitleText = stringResource(id = R.string.subtitle_properties_manage_chat),
        radioOptions = ChatHistoryRetentionOption.entries,
        initialSelectedOption = selectedOption,
        optionDescriptionMapper = @Composable {
            stringResource(id = it.stringId)
        },
        onOptionSelected = {
            selectedOption = it
        },
        confirmButtonText = if (isCustomOptionSelected) {
            stringResource(id = R.string.general_next)
        } else {
            stringResource(id = R.string.general_ok)
        },
        isConfirmButtonEnable = { isConfirmButtonEnable },
        onConfirmRequest = onConfirmClick,
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onDismissRequest = onDismissRequest,
    )
}

private fun getOptionFromRetentionTime(period: Long): ChatHistoryRetentionOption {
    if (period == Constants.DISABLED_RETENTION_TIME) {
        return ChatHistoryRetentionOption.Disabled
    }

    val days = period % Constants.SECONDS_IN_DAY
    val weeks = period % Constants.SECONDS_IN_WEEK
    val months = period % Constants.SECONDS_IN_MONTH_30

    val isOneMonthPeriod = period / Constants.SECONDS_IN_MONTH_30 == 1L
    val isOneWeekPeriod = period / Constants.SECONDS_IN_WEEK == 1L
    val isOneDayPeriod = period / Constants.SECONDS_IN_DAY == 1L

    return when {
        months == 0L && isOneMonthPeriod -> ChatHistoryRetentionOption.OneMonth

        weeks == 0L && isOneWeekPeriod -> ChatHistoryRetentionOption.OneWeek

        days == 0L && isOneDayPeriod -> ChatHistoryRetentionOption.OneDay

        else -> ChatHistoryRetentionOption.Custom
    }
}

@CombinedThemePreviews
@Composable
private fun ChatHistoryRetentionOptionsDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatHistoryRetentionConfirmationDialog(
            currentRetentionTime = 0L,
            onConfirmClick = {},
            onDismissRequest = {}
        )
    }
}

internal const val CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG =
    "chat_history_retention_time_confirmation_dialog:radio_option_set_custom_retention_time"
