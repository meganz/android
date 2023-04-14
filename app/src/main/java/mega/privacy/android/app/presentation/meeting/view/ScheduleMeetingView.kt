package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.dialog.view.SimpleDialog
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.icon
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.core.ui.controls.CustomDivider
import mega.privacy.android.core.ui.controls.MegaSwitch
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.grey_alpha_038
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.secondary_dark
import mega.privacy.android.core.ui.theme.secondary_light
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

/**
 * Schedule meeting View
 */
@Composable
internal fun ScheduleMeetingView(
    state: ScheduleMeetingState,
    onButtonClicked: (ScheduleMeetingAction) -> Unit = {},
    onDiscardClicked: () -> Unit,
    onAcceptClicked: () -> Unit,
    onStartTimeClicked: () -> Unit,
    onStartDateClicked: () -> Unit,
    onEndTimeClicked: () -> Unit,
    onEndDateClicked: () -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSnackbarShown: () -> Unit,
    onDiscardMeetingDialog: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(snackbarData = data,
                    backgroundColor = black.takeIf { isLight() } ?: white)
            }
        },
        topBar = {
            ScheduleMeetingAppBar(
                state = state,
                onAcceptClicked = onAcceptClicked,
                onDiscardClicked = onDiscardClicked,
                elevation = !firstItemVisible
            )
        }
    ) { paddingValues ->
        DiscardMeetingAlertDialog(
            state = state,
            onKeepEditing = { onDismiss() },
            onDiscard = { onDiscardMeetingDialog() })

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(paddingValues)
        ) {
            item(key = "Schedule meeting start date and time") {
                ScheduledMeetingDateAndTime(
                    state = state,
                    isStart = true,
                    onDateClicked = onStartDateClicked,
                    onTimeClicked = onStartTimeClicked
                )
            }
            item(key = "Schedule meeting end date and time") {
                ScheduledMeetingDateAndTime(
                    state = state,
                    isStart = false,
                    onDateClicked = onEndDateClicked,
                    onTimeClicked = onEndTimeClicked
                )
            }

            items(state.buttons) { button ->
                ActionButton(state = state, action = button, onButtonClicked = onButtonClicked)
            }
        }

        if (state.snackBar != null) {
            val msg = stringResource(id = state.snackBar)

            LaunchedEffect(scaffoldState.snackbarHostState) {
                val s = scaffoldState.snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )

                if (s == SnackbarResult.Dismissed) {
                    onSnackbarShown()
                }
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(!firstItemVisible)
}

@Composable
private fun isLight(): Boolean = MaterialTheme.colors.isLight

/**
 * Scheduled meeting date and time
 *
 * @param state [ScheduledMeetingInfoState]
 */
@Composable
private fun ScheduledMeetingDateAndTime(
    state: ScheduleMeetingState,
    isStart: Boolean,
    onDateClicked: () -> Unit,
    onTimeClicked: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp)
            .height(56.dp)
    ) {
        Text(
            text = if (isStart) state.getStartDate() else state.getEndDate(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable { onDateClicked() },
            style = MaterialTheme.typography.subtitle1,
            color = grey_alpha_087.takeIf { isLight() } ?: white)

        Text(
            text = if (isStart) state.getStartTime() else state.getEndTime(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable { onTimeClicked() },
            style = MaterialTheme.typography.subtitle1,
            color = grey_alpha_087.takeIf { isLight() } ?: white)
    }
}

@Composable
private fun ActionButton(
    state: ScheduleMeetingState,
    action: ScheduleMeetingAction,
    onButtonClicked: (ScheduleMeetingAction) -> Unit = {},
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            onButtonClicked(action)
        }) {
        ActionOption(
            state = state,
            action = action,
            isChecked = when (action) {
                ScheduleMeetingAction.MeetingLink -> state.enabledMeetingLinkOption
                ScheduleMeetingAction.AllowNonHostAddParticipants -> state.enabledAllowAddParticipantsOption
                ScheduleMeetingAction.SendCalendarInvite -> false
                else -> true
            },
            hasSwitch = when (action) {
                ScheduleMeetingAction.Recurrence,
                ScheduleMeetingAction.AddParticipants,
                ScheduleMeetingAction.AddDescription,
                -> false
                else -> true
            }
        )

        CustomDivider(
            withStartPadding = when (action) {
                ScheduleMeetingAction.Recurrence,
                ScheduleMeetingAction.AddParticipants,
                ScheduleMeetingAction.SendCalendarInvite,
                -> true
                else -> false
            }
        )
    }
}

/**
 * Schedule meeting App bar view
 *
 * @param state                     [ScheduledMeetingInfoState]
 * @param onAcceptClicked           When on accept scheduled meeting is clicked
 * @param onDiscardClicked          When on discard is clicked
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun ScheduleMeetingAppBar(
    state: ScheduleMeetingState,
    onAcceptClicked: () -> Unit,
    onDiscardClicked: () -> Unit,
    elevation: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp,
        color = MaterialTheme.colors.surface
    ) {
        val focusRequester = remember { FocusRequester() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    IconButton(onClick = onDiscardClicked) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_close),
                            contentDescription = "Cancel schedule meeting button",
                            tint = grey_alpha_087.takeIf { isLight() } ?: white
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = { onAcceptClicked() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_confirm),
                            contentDescription = "Accept schedule meeting button",
                            tint = if (state.meetingName == null)
                                grey_alpha_038.takeIf { isLight() } ?: white_alpha_038
                            else
                                secondary_light.takeIf { isLight() } ?: secondary_dark
                        )
                    }
                }
            }

            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .focusRequester(focusRequester),
                value = "",
                onValueChange = { },
                placeholder = {
                    Text(
                        modifier = Modifier.alpha(ContentAlpha.medium),
                        text = stringResource(id = R.string.meetings_schedule_meeting_name_hint),
                        color = grey_alpha_038.takeIf { isLight() } ?: white_alpha_038,
                        style = MaterialTheme.typography.h6,
                    )
                },
                textStyle = MaterialTheme.typography.h6,
                singleLine = true,
                leadingIcon = {},
                trailingIcon = {},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    cursorColor = MaterialTheme.colors.secondary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        DisposableEffect(Unit) {
            focusRequester.requestFocus()
            onDispose { }
        }
    }
}

/**
 * Show action buttons options
 *
 * @param state         [ScheduleMeetingState]
 * @param action        [ScheduleMeetingAction]
 * @param isChecked     True, if the option must be checked. False if not
 * @param hasSwitch     True, if the option has a switch. False if not
 */
@Composable
private fun ActionOption(
    state: ScheduleMeetingState,
    action: ScheduleMeetingAction,
    isChecked: Boolean,
    hasSwitch: Boolean,
) {
    Row(
        verticalAlignment = if (action == ScheduleMeetingAction.MeetingLink) Alignment.Top else Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RectangleShape)
                    .wrapContentSize(Alignment.TopCenter)

            ) {
                Icon(painter = painterResource(id = action.icon),
                    contentDescription = "${action.name} icon",
                    tint = grey_alpha_054.takeIf { isLight() }
                        ?: white_alpha_054)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Text(modifier = Modifier
                    .padding(start = 32.dp, end = 15.dp),
                    style = MaterialTheme.typography.subtitle1,
                    text = stringResource(id = action.title),
                    color = grey_alpha_087.takeIf { isLight() } ?: white)

                var subtitle: String? = null

                when (action) {
                    ScheduleMeetingAction.MeetingLink -> action.description?.let { description ->
                        subtitle = stringResource(id = description)
                    }
                    ScheduleMeetingAction.AddParticipants -> if (state.participantItemList.isNotEmpty()) subtitle =
                        stringResource(
                            id = R.string.number_of_participants,
                            state.participantItemList.size
                        )
                    ScheduleMeetingAction.Recurrence -> subtitle = when (state.freq) {
                        OccurrenceFrequencyType.Invalid -> stringResource(id = R.string.meetings_schedule_meeting_recurrence_never_label)
                        OccurrenceFrequencyType.Daily,
                        OccurrenceFrequencyType.Weekly,
                        OccurrenceFrequencyType.Monthly,
                        -> null
                    }

                    else -> {}
                }

                subtitle?.let {
                    Text(modifier = Modifier
                        .padding(
                            start = 32.dp,
                            end = 16.dp,
                            top = if (action == ScheduleMeetingAction.MeetingLink) 10.dp else if (action == ScheduleMeetingAction.Recurrence) 6.dp else 2.dp
                        ),
                        style = MaterialTheme.typography.body2,
                        text = it,
                        color = grey_alpha_054.takeIf { isLight() } ?: white_alpha_054)
                }
            }
        }

        if (hasSwitch) {
            Box(
                modifier = Modifier
                    .wrapContentSize(if (action == ScheduleMeetingAction.MeetingLink) Alignment.TopEnd else Alignment.Center)
            ) {
                MegaSwitch(
                    checked = isChecked,
                    enabled = action != ScheduleMeetingAction.SendCalendarInvite,
                    onCheckedChange = null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * Dialogue to discard the meeting
 *
 * @param state                     [ScheduledMeetingInfoState]
 * @param onKeepEditing             When continue editing the meeting.
 * @param onDiscard                 When discard the meeting.
 */
@Composable
private fun DiscardMeetingAlertDialog(
    state: ScheduleMeetingState,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    if (state.discardMeetingDialog) {
        SimpleDialog(
            title = null,
            description = R.string.meetings_schedule_meeting_discard_meeting_dialog_title,
            confirmButton = R.string.meetings_schedule_meeting_discard_meeting_dialog_discard_option,
            dismissButton = R.string.meetings_schedule_meeting_discard_meeting_dialog_keep_editing_option,
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
            onDismiss = onKeepEditing,
            onConfirmButton = onDiscard
        )
    }
}

/**
 * Discard Meeting Alert Dialog Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewDiscardMeetingAlertDialog")
@Composable
fun PreviewDiscardMeetingAlertDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DiscardMeetingAlertDialog(state = ScheduleMeetingState(
            meetingName = "Title meeting",
            freq = OccurrenceFrequencyType.Invalid,
            startDate = null,
            endDate = null,
            participantItemList = emptyList(),
            finish = false,
            buttons = ScheduleMeetingAction.values().asList(),
            snackBar = null,
            discardMeetingDialog = true,
        ),
            onKeepEditing = { },
            onDiscard = {})
    }
}

/**
 * Schedule meeting View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewScheduleMeetingView")
@Composable
private fun PreviewScheduleMeetingView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ScheduleMeetingView(
            state = ScheduleMeetingState(
                meetingName = "Title meeting",
                freq = OccurrenceFrequencyType.Invalid,
                startDate = null,
                endDate = null,
                participantItemList = emptyList(),
                finish = false,
                buttons = ScheduleMeetingAction.values().asList(),
                snackBar = null
            ),
            onButtonClicked = {},
            onDiscardClicked = {},
            onAcceptClicked = {},
            onStartTimeClicked = {},
            onStartDateClicked = {},
            onEndTimeClicked = {},
            onEndDateClicked = {},
            onScrollChange = {},
            onDismiss = {},
            onSnackbarShown = {},
            onDiscardMeetingDialog = {}
        )
    }
}


