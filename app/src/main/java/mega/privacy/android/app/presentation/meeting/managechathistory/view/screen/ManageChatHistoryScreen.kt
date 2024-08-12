package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog.ChatHistoryRetentionConfirmationDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ManageChatHistoryRoute(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageChatHistoryViewModel = hiltViewModel(),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        onNavigateUp()
    }

    LaunchedEffect(uiState.shouldNavigateUp) {
        if (uiState.shouldNavigateUp) {
            onNavigateUp()
            viewModel.onNavigatedUp()
        }
    }

    LaunchedEffect(uiState.statusMessageResId) {
        uiState.statusMessageResId?.let {
            snackBarHostState.showAutoDurationSnackbar(
                message = context.getString(it)
            )
            viewModel.onStatusMessageDisplayed()
        }
    }

    Box(modifier = modifier.semantics { testTagsAsResourceId = true }) {
        ManageChatHistoryScreen(
            modifier = Modifier.systemBarsPadding().fillMaxSize(),
            uiState = uiState,
            onNavigateUp = onNavigateUp,
            onConfirmClearChatClick = viewModel::clearChatHistory,
            onSetChatRetentionTime = viewModel::setChatRetentionTime,
            snackbarHostState = snackBarHostState
        )
    }
}

@Composable
internal fun ManageChatHistoryScreen(
    uiState: ManageChatHistoryUIState,
    onNavigateUp: () -> Unit,
    onConfirmClearChatClick: (chatRoomId: Long) -> Unit,
    onSetChatRetentionTime: (period: Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var shouldShowCustomTimePicker by rememberSaveable { mutableStateOf(false) }
    var shouldShowClearChatConfirmation by rememberSaveable { mutableStateOf(false) }
    var shouldShowHistoryRetentionConfirmation by rememberSaveable { mutableStateOf(false) }
    val timePickerState = rememberCustomRetentionTimePickerState()

    Scaffold(
        modifier = modifier,
        scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
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
                onCheckChange = { isChecked ->
                    if (isChecked) {
                        shouldShowHistoryRetentionConfirmation = true
                    } else {
                        onSetChatRetentionTime(DISABLED_RETENTION_TIME)
                    }
                },
                onHistoryClearingOptionSubtitleClick = {
                    shouldShowHistoryRetentionConfirmation = true
                }
            )

            if (shouldShowCustomTimePicker) {
                CustomRetentionTimePicker(
                    ordinalPickerState = timePickerState.ordinalTimePickerItem,
                    periodPickerState = timePickerState.periodTimePickerItem,
                    onOrdinalPickerValueChange = timePickerState::onOrdinalPickerValueChange,
                    onPeriodPickerValueChange = { _, newValue ->
                        timePickerState.onPeriodPickerValueChange(newValue)
                    },
                    onOrdinalPickerScrollChange = { scrollState ->
                        if (scrollState == NumberPickerScrollState.Idle) {
                            timePickerState.onCustomPickerScrollChange()
                        }
                    },
                    onPeriodPickerScrollChange = { scrollState ->
                        if (scrollState == NumberPickerScrollState.Idle) {
                            timePickerState.onCustomPickerScrollChange()
                        }
                    },
                    onOKClick = {
                        onSetChatRetentionTime(timePickerState.getTotalSeconds())
                        shouldShowCustomTimePicker = false
                    }
                )
            }

            ClearHistoryOption(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        shouldShowClearChatConfirmation = true
                    }
                    .testTag(CLEAR_HISTORY_OPTION_TAG),
                title = if (uiState.chatRoom?.isMeeting == true) {
                    stringResource(id = R.string.meetings_manage_history_clear)
                } else {
                    stringResource(id = R.string.title_properties_clear_chat_history)
                }
            )
        }
    }

    if (shouldShowClearChatConfirmation) {
        uiState.chatRoom?.apply {
            ClearChatConfirmationDialog(
                isMeeting = isMeeting,
                onConfirm = {
                    onConfirmClearChatClick(chatId)
                    shouldShowClearChatConfirmation = false
                },
                onDismiss = {
                    shouldShowClearChatConfirmation = false
                }
            )
        }
    }

    if (shouldShowHistoryRetentionConfirmation) {
        ChatHistoryRetentionConfirmationDialog(
            currentRetentionTime = uiState.retentionTime,
            onDismissRequest = {
                shouldShowHistoryRetentionConfirmation = false
            },
            onConfirmClick = {
                shouldShowHistoryRetentionConfirmation = false
                if (it == ChatHistoryRetentionOption.Custom) {
                    shouldShowCustomTimePicker = true
                    timePickerState.initializeByRetentionTime(uiState.retentionTime)
                } else {
                    onSetChatRetentionTime(getSecondsFromRetentionTimeOption(it))
                }
            }
        )
    }
}

private fun getSecondsFromRetentionTimeOption(option: ChatHistoryRetentionOption) =
    when (option) {
        ChatHistoryRetentionOption.OneDay -> Constants.SECONDS_IN_DAY.toLong()
        ChatHistoryRetentionOption.OneWeek -> Constants.SECONDS_IN_WEEK.toLong()
        ChatHistoryRetentionOption.OneMonth -> Constants.SECONDS_IN_MONTH_30.toLong()
        else -> DISABLED_RETENTION_TIME
    }

@Composable
private fun HistoryClearingOption(
    retentionTime: Long,
    onCheckChange: (value: Boolean) -> Unit,
    onHistoryClearingOptionSubtitleClick: () -> Unit,
) {
    val context = LocalContext.current
    val formattedRetentionTime = getRetentionTimeString(
        context = context,
        timeInSeconds = retentionTime
    )
    val isChecked = rememberSaveable(retentionTime) {
        retentionTime != DISABLED_RETENTION_TIME
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1F)) {
            MegaText(
                text = stringResource(id = R.string.title_properties_history_retention),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle1,
            )

            MegaText(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (!formattedRetentionTime.isNullOrBlank()) {
                                onHistoryClearingOptionSubtitleClick()
                            }
                        }
                    )
                    .testTag(HISTORY_CLEARING_OPTION_SUBTITLE_TAG),
                text = if (!formattedRetentionTime.isNullOrBlank()) {
                    stringResource(id = R.string.subtitle_properties_manage_chat)
                } else {
                    stringResource(id = R.string.subtitle_properties_history_retention)
                },
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
            )

            if (!formattedRetentionTime.isNullOrBlank()) {
                MegaText(
                    text = formattedRetentionTime,
                    textColor = TextColor.Accent,
                    style = MaterialTheme.typography.subtitle2
                )
            }
        }

        MegaSwitch(
            modifier = Modifier.testTag(HISTORY_CLEARING_OPTION_SWITCH_TAG),
            checked = isChecked,
            onCheckedChange = onCheckChange,
        )
    }

    MegaDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        dividerType = DividerType.FullSize
    )
}

@Composable
private fun ColumnScope.CustomRetentionTimePicker(
    ordinalPickerState: TimePickerItemState,
    periodPickerState: TimePickerItemState,
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
        with(ordinalPickerState) {
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
        with(periodPickerState) {
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
private fun List<DisplayValueState>.inStrings(): List<String> = map {
    when (it) {
        is DisplayValueState.PluralString -> pluralStringResource(
            it.id,
            it.quantity,
            it.quantity
        ).lowercase(Locale.getDefault())

        is DisplayValueState.SingularString -> {
            stringResource(id = it.id).lowercase(Locale.getDefault())
        }
    }
}

@Composable
private fun ClearHistoryOption(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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
}

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenWithRetentionTimePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ManageChatHistoryScreen(
            uiState = ManageChatHistoryUIState(retentionTime = 3600L),
            onNavigateUp = {},
            onConfirmClearChatClick = {},
            onSetChatRetentionTime = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenWithoutRetentionTimePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ManageChatHistoryScreen(
            uiState = ManageChatHistoryUIState(),
            onNavigateUp = {},
            onConfirmClearChatClick = {},
            onSetChatRetentionTime = {},
        )
    }
}

internal const val CUSTOM_TIME_PICKER_TAG =
    "manage_chat_history_screen:custom_time_picker_custom_retention_time_picker"
internal const val HISTORY_CLEARING_OPTION_SUBTITLE_TAG =
    "history_clearing_option:history_clearing_option_subtitle_display_the_clearing_option_subtitle"
internal const val HISTORY_CLEARING_OPTION_SWITCH_TAG =
    "history_clearing_option:switch_enablement_toggle"
internal const val CLEAR_HISTORY_OPTION_TAG =
    "manage_chat_history_screen:clear_history_option_the_clear_all_chat_or_meeting_history_option"
