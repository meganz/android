package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.R as CoreUiR
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getDateFormatted
import mega.privacy.android.app.presentation.extensions.getEndTimeFormatted
import mega.privacy.android.app.presentation.extensions.getStartTimeFormatted
import mega.privacy.android.app.presentation.extensions.getTimeFormatted
import mega.privacy.android.app.presentation.meeting.dialog.view.RecurringMeetingOccurrenceBottomSheetView
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.legacy.core.ui.controls.dialogs.EditOccurrenceDialog
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

/**
 * Recurring meeting info View
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RecurringMeetingInfoView(
    state: RecurringMeetingInfoState,
    managementState: ScheduledMeetingManagementState,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onOccurrenceClicked: (ChatScheduledMeetingOccurr) -> Unit,
    onSeeMoreClicked: () -> Unit,
    onCancelOccurrenceClicked: () -> Unit,
    onEditOccurrenceClicked: () -> Unit,
    onConsumeSelectOccurrenceEvent: () -> Unit,
    onResetSnackbarMessage: () -> Unit,
    onCancelOccurrence: () -> Unit = {},
    onCancelOccurrenceAndMeeting: () -> Unit = {},
    onEditOccurrence: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onDateTap: () -> Unit,
    onStartTimeTap: () -> Unit,
    onEndTimeTap: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true,
    )

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch { modalSheetState.hide() }
    }

    EventEffect(
        event = managementState.selectOccurrenceEvent,
        onConsumed = onConsumeSelectOccurrenceEvent,
        action = { modalSheetState.show() }
    )

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(snackbarData = data,
                    backgroundColor = black.takeIf { isLight } ?: white)
            }
        },
        topBar = {
            RecurringMeetingInfoAppBar(
                state = state,
                onBackPressed = onBackPressed,
                elevation = !firstItemVisible
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(paddingValues)
                .testTag("Occurrence_list_view")
        ) {
            item(key = "Occurrences list") {
                state.occurrencesList.indices.forEach { i ->
                    OccurrenceItemView(
                        modifier = Modifier.testTag("Occurrence_item_view"),
                        state = state,
                        occurrence = state.occurrencesList[i],
                        onOccurrenceClicked = onOccurrenceClicked
                    )
                }
                if (state.showSeeMoreButton) {
                    SeeMoreOccurrencesButton(
                        onSeeMoreClicked = onSeeMoreClicked
                    )
                }
            }
        }

        EventEffect(
            event = managementState.snackbarMessageContent,
            onConsumed = onResetSnackbarMessage
        ) {
            scaffoldState.snackbarHostState.showSnackbar(it)
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

    onScrollChange(!firstItemVisible)

    if (managementState.editOccurrenceTapped) {
        managementState.editedOccurrence?.let {
            val is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
            EditOccurrenceDialog(
                title = stringResource(id = R.string.meetings_update_scheduled_meeting_occurrence_dialog_title),
                confirmButtonText = stringResource(id = R.string.meetings_edit_scheduled_meeting_occurrence_dialog_confirm_button),
                cancelButtonText = stringResource(id = R.string.button_cancel),
                dateTitleText = stringResource(id = R.string.meetings_update_scheduled_meeting_occurrence_dialog_date_section),
                dateText = it.getDateFormatted() ?: "",
                startTimeTitleText = stringResource(id = R.string.meetings_update_scheduled_meeting_occurrence_dialog_start_time_section),
                endTimeTitleText = stringResource(id = R.string.meetings_update_scheduled_meeting_occurrence_dialog_end_time_section),
                startTimeText = it.getStartTimeFormatted(is24HourFormat),
                endTimeText = it.getEndTimeFormatted(is24HourFormat),
                onConfirm = onEditOccurrence,
                onDismiss = onDismissDialog,
                isConfirmButtonEnabled = managementState.isEditionValid(),
                isDateEdited = managementState.isDateEdited(),
                isStartTimeEdited = managementState.isStartTimeEdited(is24HourFormat),
                isEndTimeEdited = managementState.isEndTimeEdited(is24HourFormat),
                onDateTap = onDateTap,
                onStartTimeTap = onStartTimeTap,
                onEndTimeTap = onEndTimeTap
            )
        }
    }

    if (state.occurrencesList.size > 1 && managementState.cancelOccurrenceTapped) {
        managementState.selectedOccurrence?.let { occurrence ->
            CancelScheduledMeetingOccurrenceDialog(
                occurrence = occurrence,
                onConfirm = onCancelOccurrence,
                onDismiss = onDismissDialog,
            )
        }
    }

    managementState.isChatHistoryEmpty?.let { isChatHistoryEmpty ->
        CancelOccurrenceAndMeetingDialog(
            isChatHistoryEmpty = isChatHistoryEmpty,
            onConfirm = onCancelOccurrenceAndMeeting,
            onDismiss = onDismissDialog,
        )
    }

    RecurringMeetingOccurrenceBottomSheetView(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        meetingState = state,
        occurrence = managementState.selectedOccurrence,
        onCancelClick = onCancelOccurrenceClicked,
        onEditClick = onEditOccurrenceClicked
    )
}

/**
 * Recurring meeting info App bar view
 *
 * @param state                     [RecurringMeetingInfoState]
 * @param onBackPressed             When on back pressed option is clicked
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun RecurringMeetingInfoAppBar(
    state: RecurringMeetingInfoState,
    onBackPressed: () -> Unit,
    elevation: Boolean,
) {
    val isLight = MaterialTheme.colors.isLight
    val iconColor = black.takeIf { isLight } ?: white
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.schedTitle?.let { title ->
                        Text(text = title,
                            style = MaterialTheme.typography.subtitle1,
                            color = black.takeIf { isLight } ?: white,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                if (state.typeOccurs != OccurrenceFrequencyType.Invalid) {
                    Text(text = when (state.typeOccurs) {
                        OccurrenceFrequencyType.Daily -> stringResource(id = R.string.meetings_recurring_meeting_info_occurs_daily_subtitle)
                        OccurrenceFrequencyType.Weekly -> stringResource(id = R.string.meetings_recurring_meeting_info_occurs_weekly_subtitle)
                        OccurrenceFrequencyType.Monthly -> stringResource(id = R.string.meetings_recurring_meeting_info_occurs_monthly_subtitle)
                        else -> ""
                    },
                        style = MaterialTheme.typography.subtitle2,
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }

            }
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = iconColor
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

/**
 * View of a occurrence in the list
 *
 * @param state                    [RecurringMeetingInfoState]
 * @param occurrence               [ChatScheduledMeetingOccurr]
 * @param onOccurrenceClicked      Detect when a occurrence is clicked
 */
@Composable
private fun OccurrenceItemView(
    modifier: Modifier = Modifier,
    state: RecurringMeetingInfoState,
    occurrence: ChatScheduledMeetingOccurr,
    onOccurrenceClicked: (ChatScheduledMeetingOccurr) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    Column {
        occurrence.getDateFormatted()?.let { date ->
            Row(
                modifier = Modifier
                    .testTag("Occurrence_item_view_date_formatted")
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = date,
                    style = MaterialTheme.typography.body2,
                    color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }

        Row(
            modifier = modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = modifier
                    .weight(1f)
                    .height(72.dp)
            ) {
                Column(
                    modifier = modifier
                        .padding(start = 17.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Box(
                        modifier
                            .size(40.dp)
                            .background(Color.Transparent)
                    ) {
                        RecurringMeetingAvatarView(state = state)
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = 17.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.schedTitle?.let { title ->
                            Text(
                                modifier = Modifier.testTag("Occurrence_item_view_title"),
                                text = title,
                                style = MaterialTheme.typography.subtitle1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    occurrence.startDateTime?.let {
                        Text(text = occurrence.getTimeFormatted(state.is24HourFormat),
                            color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                            style = MaterialTheme.typography.subtitle2)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.CenterEnd)
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 30.dp, end = 20.dp)
                            .clickable { onOccurrenceClicked(occurrence) },
                        painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
                        contentDescription = "Three dots icon",
                        tint = grey_alpha_054.takeIf { isLight } ?: white_alpha_054
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(start = 16.dp),
            color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
            thickness = 1.dp)
    }
}

/**
 * See more occurrences in the list button view
 *
 * @param onSeeMoreClicked      Detect when see more button is clicked
 */
@Composable
private fun SeeMoreOccurrencesButton(
    onSeeMoreClicked: () -> Unit,
) {
    Row(modifier = Modifier
        .clickable { onSeeMoreClicked() }
        .fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                imageVector = ImageVector.vectorResource(id = CoreUiR.drawable.ic_chevron_down),
                contentDescription = "See more Icon",
                tint = MaterialTheme.colors.secondary
            )
            Text(
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.button,
                text = stringResource(id = R.string.meetings_recurring_meeting_info_see_more_occurrences_button),
                color = MaterialTheme.colors.secondary
            )
        }
    }
}
