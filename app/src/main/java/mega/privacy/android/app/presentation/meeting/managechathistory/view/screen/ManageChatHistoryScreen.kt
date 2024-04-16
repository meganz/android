package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.presentation.meeting.managechathistory.component.NumberPicker
import mega.privacy.android.app.presentation.meeting.managechathistory.component.NumberPickerScrollState
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.model.DisplayValueUiState
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.meeting.managechathistory.model.TimePickerItemUiState
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.theme.MegaAppTheme
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ManageChatHistoryRoute(
    onRetryConnectionsAndSignalPresence: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageChatHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        onRetryConnectionsAndSignalPresence()
        onNavigateUp()
    }

    LaunchedEffect(uiState.shouldNavigateUp) {
        if (uiState.shouldNavigateUp) {
            onNavigateUp()
            viewModel.onNavigatedUp()
        }
    }

    ManageChatHistoryScreen(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onConfirmClearChatClick = {
            viewModel.apply {
                dismissClearChatConfirmation()
                clearChatHistory(it)
            }
        },
        onClearChatConfirmationDismiss = viewModel::dismissClearChatConfirmation,
        onRetentionTimeOptionSelected = viewModel::updateHistoryRetentionTimeConfirmation,
        onConfirmRetentionTimeClick = {
            viewModel.apply {
                onNewRetentionTimeOptionConfirmed(it)
                dismissHistoryRetentionConfirmation()
            }
        },
        onRetentionTimeConfirmationDismiss = viewModel::dismissHistoryRetentionConfirmation,
        onHistoryClearingCheckChange = {},
        onCustomTimePickerClick = viewModel::onCustomTimePickerConfirmed
    )
}

@Composable
internal fun ManageChatHistoryScreen(
    uiState: ManageChatHistoryUIState,
    onNavigateUp: () -> Unit,
    onConfirmClearChatClick: (chatRoomId: Long) -> Unit,
    onClearChatConfirmationDismiss: () -> Unit,
    onRetentionTimeOptionSelected: (option: ChatHistoryRetentionOption) -> Unit,
    onConfirmRetentionTimeClick: (option: ChatHistoryRetentionOption) -> Unit,
    onRetentionTimeConfirmationDismiss: () -> Unit,
    onHistoryClearingCheckChange: (value: Boolean) -> Unit,
    onCustomTimePickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = if (uiState.chatRoom?.isMeeting == true) {
                    stringResource(id = R.string.meetings_manage_history_view_title)
                } else {
                    stringResource(id = R.string.title_properties_manage_chat)
                },
                onNavigationPressed = onNavigateUp,
                elevation = 0.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            HistoryClearingOption(
                retentionTime = uiState.retentionTime,
                isChecked = uiState.isHistoryClearingOptionChecked,
                onCheckChange = onHistoryClearingCheckChange,
            )

            if (uiState.shouldShowCustomTimePicker) {
                CustomRetentionTimePicker(
                    ordinalPickerUiState = uiState.ordinalTimePickerItem,
                    periodPickerUiState = uiState.periodTimePickerItem,
                    onOrdinalPickerValueChange = { oldValue, newValue -> },
                    onPeriodPickerValueChange = { oldValue, newValue -> },
                    onOrdinalPickerScrollChange = { },
                    onPeriodPickerScrollChange = { },
                    onOKClick = onCustomTimePickerClick
                )
            }

            ClearMeetingHistoryOption(
                title = if (uiState.chatRoom?.isMeeting == true) {
                    stringResource(id = R.string.meetings_manage_history_clear)
                } else {
                    stringResource(id = R.string.title_properties_clear_chat_history)
                }
            )
        }
    }

    if (uiState.shouldShowClearChatConfirmation) {
        uiState.chatRoom?.apply {
            ClearChatConfirmationDialog(
                isMeeting = isMeeting,
                onConfirm = { onConfirmClearChatClick(chatId) },
                onDismiss = onClearChatConfirmationDismiss
            )
        }
    }

    if (uiState.shouldShowHistoryRetentionConfirmation) {
        ConfirmationDialogWithRadioButtons(
            modifier = Modifier.testTag(CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG),
            titleText = stringResource(id = R.string.title_properties_history_retention),
            subTitleText = stringResource(id = R.string.subtitle_properties_manage_chat),
            radioOptions = ChatHistoryRetentionOption.entries,
            initialSelectedOption = uiState.selectedHistoryRetentionTimeOption,
            optionDescriptionMapper = @Composable {
                stringResource(id = it.stringId)
            },
            onOptionSelected = onRetentionTimeOptionSelected,
            confirmButtonText = stringResource(id = uiState.confirmButtonStringId),
            isConfirmButtonEnable = { uiState.isConfirmButtonEnable },
            onConfirmRequest = onConfirmRetentionTimeClick,
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onDismissRequest = onRetentionTimeConfirmationDismiss,
        )
    }
}

@Composable
private fun ColumnScope.HistoryClearingOption(
    retentionTime: Long,
    isChecked: Boolean,
    onCheckChange: (value: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val formattedRetentionTime = getRetentionTimeString(
        context = context,
        timeInSeconds = retentionTime
    )
    GenericTwoLineListItem(
        modifier = Modifier.padding(top = 4.dp),
        title = stringResource(id = R.string.title_properties_history_retention),
        subtitle = if (!formattedRetentionTime.isNullOrBlank()) {
            stringResource(id = R.string.subtitle_properties_manage_chat)
        } else {
            stringResource(id = R.string.subtitle_properties_history_retention)
        },
        showEntireSubtitle = true,
        trailingIcons = {
            MegaSwitch(
                modifier = Modifier.padding(end = 4.dp),
                checked = isChecked,
                onCheckedChange = onCheckChange,
            )
        }
    )

    if (!formattedRetentionTime.isNullOrBlank()) {
        MegaText(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            text = formattedRetentionTime,
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.subtitle2
        )
    }

    MegaDivider(
        modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
        dividerType = DividerType.FullSize
    )
}

@Composable
private fun ColumnScope.CustomRetentionTimePicker(
    ordinalPickerUiState: TimePickerItemUiState,
    periodPickerUiState: TimePickerItemUiState,
    onOrdinalPickerValueChange: (oldValue: Int, newValue: Int) -> Unit,
    onPeriodPickerValueChange: (oldValue: Int, newValue: Int) -> Unit,
    onOrdinalPickerScrollChange: (scrollState: NumberPickerScrollState) -> Unit,
    onPeriodPickerScrollChange: (scrollState: NumberPickerScrollState) -> Unit,
    onOKClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .height(126.dp)
            .testTag(CUSTOM_TIME_PICKER_TAG),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ordinal picker (1, 2, 3, 4, ...)
        with(ordinalPickerUiState) {
            NumberPicker(
                minimumValue = minimumValue,
                maximumValue = maximumValue,
                currentValue = currentValue,
                displayValues = displayValues?.inStrings(),
                onValueChange = onOrdinalPickerValueChange,
                onScrollChange = onOrdinalPickerScrollChange
            )
        }

        Spacer(modifier = Modifier.width(34.dp))

        // Period picker (hour, day, week, day, month, year)
        with(periodPickerUiState) {
            NumberPicker(
                minimumValue = minimumValue,
                maximumValue = maximumValue,
                currentValue = currentValue,
                displayValues = displayValues?.inStrings(),
                onValueChange = onPeriodPickerValueChange,
                onScrollChange = onPeriodPickerScrollChange
            )
        }
    }

    TextMegaButton(
        modifier = Modifier.align(Alignment.End),
        text = stringResource(id = R.string.general_ok),
        onClick = onOKClick
    )

    MegaDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        dividerType = DividerType.FullSize
    )
}

@Composable
private fun List<DisplayValueUiState>.inStrings(): List<String> = map {
    when (it) {
        is DisplayValueUiState.PluralString -> pluralStringResource(
            it.id,
            it.quantity,
            it.quantity
        ).lowercase(Locale.getDefault())

        is DisplayValueUiState.SingularString -> {
            stringResource(id = it.id).lowercase(Locale.getDefault())
        }
    }
}

@Composable
private fun ColumnScope.ClearMeetingHistoryOption(title: String) {
    GenericTwoLineListItem(
        modifier = Modifier.padding(vertical = 4.dp),
        title = title,
        titleTextColor = TextColor.Error,
        subtitle = stringResource(id = R.string.subtitle_properties_chat_clear),
        showEntireSubtitle = true
    )

    MegaDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        dividerType = DividerType.FullSize
    )
}

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenWithRetentionTimePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ManageChatHistoryScreen(
            uiState = ManageChatHistoryUIState(retentionTime = 3600L),
            onNavigateUp = {},
            onConfirmClearChatClick = {},
            onClearChatConfirmationDismiss = {},
            onRetentionTimeOptionSelected = {},
            onConfirmRetentionTimeClick = {},
            onRetentionTimeConfirmationDismiss = {},
            onHistoryClearingCheckChange = {},
            onCustomTimePickerClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenWithoutRetentionTimePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ManageChatHistoryScreen(
            uiState = ManageChatHistoryUIState(),
            onNavigateUp = {},
            onConfirmClearChatClick = {},
            onClearChatConfirmationDismiss = {},
            onRetentionTimeOptionSelected = {},
            onConfirmRetentionTimeClick = {},
            onRetentionTimeConfirmationDismiss = {},
            onHistoryClearingCheckChange = {},
            onCustomTimePickerClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenWithCustomPickerPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ManageChatHistoryScreen(
            uiState = ManageChatHistoryUIState(
                shouldShowCustomTimePicker = true
            ),
            onNavigateUp = {},
            onConfirmClearChatClick = {},
            onClearChatConfirmationDismiss = {},
            onRetentionTimeOptionSelected = {},
            onConfirmRetentionTimeClick = {},
            onRetentionTimeConfirmationDismiss = {},
            onHistoryClearingCheckChange = {},
            onCustomTimePickerClick = {}
        )
    }
}

internal const val CHAT_HISTORY_RETENTION_TIME_CONFIRMATION_TAG =
    "manage_chat_history_screen:chat_history_retention_time_confirmation_set_custom_retention_time"
internal const val CUSTOM_TIME_PICKER_TAG =
    "manage_chat_history_screen:custom_time_picker_custom_retention_time_picker"
