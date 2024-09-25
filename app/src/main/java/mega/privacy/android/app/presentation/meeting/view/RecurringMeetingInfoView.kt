package mega.privacy.android.app.presentation.meeting.view

import mega.privacy.android.core.R as CoreUiR
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getDateFormatted
import mega.privacy.android.app.presentation.extensions.getEndTimeFormatted
import mega.privacy.android.app.presentation.extensions.getEndZoneDateTime
import mega.privacy.android.app.presentation.extensions.getStartTimeFormatted
import mega.privacy.android.app.presentation.extensions.getStartZoneDateTime
import mega.privacy.android.app.presentation.extensions.getTimeFormatted
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.view.dialog.CancelOccurrenceAndMeetingDialog
import mega.privacy.android.app.presentation.meeting.view.dialog.CancelScheduledMeetingOccurrenceDialog
import mega.privacy.android.app.presentation.meeting.view.sheet.RecurringMeetingOccurrenceBottomSheetView
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.legacy.core.ui.controls.dialogs.EditOccurrenceDialog
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_087
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Recurring meeting info View
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RecurringMeetingInfoView(
    state: RecurringMeetingInfoState,
    managementState: ScheduledMeetingManagementUiState,
    onBackPressed: () -> Unit,
    onOccurrenceClicked: (ChatScheduledMeetingOccurr) -> Unit,
    onSeeMoreClicked: () -> Unit,
    onCancelOccurrenceClicked: () -> Unit,
    onEditOccurrenceClicked: () -> Unit,
    onConsumeSelectOccurrenceEvent: () -> Unit,
    onResetSnackbarMessage: () -> Unit,
    onDateTap: () -> Unit,
    onStartTimeTap: () -> Unit,
    onEndTimeTap: () -> Unit,
    onUpgradeNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onCancelOccurrence: () -> Unit = {},
    onCancelOccurrenceAndMeeting: () -> Unit = {},
    onEditOccurrence: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch { modalSheetState.hide() }
    }

    EventEffect(
        event = managementState.selectOccurrenceEvent,
        onConsumed = onConsumeSelectOccurrenceEvent,
        action = { modalSheetState.show() }
    )

    MegaScaffold(
        modifier = modifier.navigationBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            RecurringMeetingInfoAppBar(
                state = state,
                onBackPressed = onBackPressed,
            )
        },
        scrollableContentState = listState
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
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(it)
        }
    }

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
                freePlanLimitationWarningText = stringResource(id = R.string.meetings_edit_occurrence_free_plan_60_minute_limit_warning),
                shouldShowFreePlanLimitWarning = managementState.shouldShowFreePlanLimitWarning(
                    managementState.editedOccurrence.getStartZoneDateTime(),
                    managementState.editedOccurrence.getEndZoneDateTime()
                ),
                onConfirm = onEditOccurrence,
                onDismiss = onDismissDialog,
                isConfirmButtonEnabled = managementState.isEditionValid(),
                isDateEdited = managementState.isDateEdited(),
                isStartTimeEdited = managementState.isStartTimeEdited(is24HourFormat),
                isEndTimeEdited = managementState.isEndTimeEdited(is24HourFormat),
                onDateTap = onDateTap,
                onStartTimeTap = onStartTimeTap,
                onEndTimeTap = onEndTimeTap,
                onUpgradeNowClicked = onUpgradeNowClicked,
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
 */
@Composable
private fun RecurringMeetingInfoAppBar(
    state: RecurringMeetingInfoState,
    onBackPressed: () -> Unit,
) {
    val context = LocalContext.current
    val subtitle = remember(state.typeOccurs) {
        when (state.typeOccurs) {
            OccurrenceFrequencyType.Daily -> context.getString(R.string.meetings_recurring_meeting_info_occurs_daily_subtitle)
            OccurrenceFrequencyType.Weekly -> context.getString(R.string.meetings_recurring_meeting_info_occurs_weekly_subtitle)
            OccurrenceFrequencyType.Monthly -> context.getString(R.string.meetings_recurring_meeting_info_occurs_monthly_subtitle)
            else -> ""
        }
    }
    MegaAppBar(
        appBarType = AppBarType.BACK_NAVIGATION,
        onNavigationPressed = onBackPressed,
        title = state.schedTitle.orEmpty(),
        subtitle = subtitle,
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
    state: RecurringMeetingInfoState,
    occurrence: ChatScheduledMeetingOccurr,
    onOccurrenceClicked: (ChatScheduledMeetingOccurr) -> Unit,
    modifier: Modifier = Modifier,
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
