package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.meeting.DropdownType
import mega.privacy.android.app.presentation.extensions.meeting.StringId
import mega.privacy.android.app.presentation.meeting.model.CreateScheduledMeetingState
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.core.ui.controls.chips.DropdownMenuChip
import mega.privacy.android.core.ui.controls.chips.TextButtonChip
import mega.privacy.android.core.ui.controls.chips.TextFieldChip
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType

/**
 * Custom recurrence View
 */
@Composable
internal fun CustomRecurrenceView(
    state: CreateScheduledMeetingState,
    onScrollChange: (Boolean) -> Unit,
    onAcceptClicked: () -> Unit,
    onRejectClicked: () -> Unit,
    onTypeClicked: (DropdownOccurrenceType) -> Unit,
    onNumberClicked: (String) -> Unit,
    onWeekdaysClicked: () -> Unit,
    onFocusChanged: () -> Unit,
) {
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.onPrimary
                )
            }
        },
        topBar = {
            CustomRecurrenceAppBar(
                isValidRecurrence = state.customRecurrenceState.isValidRecurrence,
                onAcceptClicked = onAcceptClicked,
                onRejectClicked = onRejectClicked,
                elevation = !firstItemVisible
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(paddingValues)
        ) {
            item(key = "Occurs every") {
                OccursEverySection(
                    modifier = Modifier,
                    interval = state.customRecurrenceState.newRules.interval,
                    isWeekdaysSelected = state.customRecurrenceState.isWeekdaysSelected,
                    dropdownOccurrenceType = state.customRecurrenceState.newRules.freq.DropdownType,
                    onTypeClicked = onTypeClicked,
                    onNumberClicked = onNumberClicked,
                    onFocusChanged = onFocusChanged
                )
            }

            if (state.customRecurrenceState.dropdownOccurrenceType == DropdownOccurrenceType.Day) {
                item(key = "Occurs daily") {
                    OccursDailySection(
                        modifier = Modifier,
                        isWeekdaysSelected = state.customRecurrenceState.isWeekdaysSelected,
                        onWeekdaysClicked = onWeekdaysClicked,
                    )
                }
            }
        }
    }

    onScrollChange(!firstItemVisible)
}

/**
 * Custom recurrence App bar view
 *
 * @param isValidRecurrence         True if it is valid. False, if it does not.
 * @param onAcceptClicked           When on accept recurrence is clicked
 * @param onRejectClicked           When on back pressed
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun CustomRecurrenceAppBar(
    isValidRecurrence: Boolean,
    onAcceptClicked: () -> Unit,
    onRejectClicked: () -> Unit,
    elevation: Boolean,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.meetings_schedule_meeting_recurrence_label),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(modifier = Modifier.testTag(TEST_TAG_BACK_ICON), onClick = onRejectClicked) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        },
        actions = {
            IconButton(modifier = Modifier.testTag(TEST_TAG_ACCEPT_ICON), onClick = {
                if (isValidRecurrence) {
                    onAcceptClicked()
                }
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_confirm),
                    contentDescription = "Accept custom recurrence button",
                    tint = if (isValidRecurrence) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.grey_alpha_038_white_alpha_038
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

/**
 * Occurs every option
 *
 * @param interval
 * @param isWeekdaysSelected
 * @param dropdownOccurrenceType
 */
@Composable
private fun OccursEverySection(
    interval: Int,
    isWeekdaysSelected: Boolean,
    dropdownOccurrenceType: DropdownOccurrenceType?,
    onNumberClicked: (String) -> Unit,
    onTypeClicked: (DropdownOccurrenceType) -> Unit,
    onFocusChanged: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .testTag(TEST_TAG_OCCURS_EVERY)
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 23.dp, top = 17.dp, end = 16.dp)
    ) {
        Text(
            modifier = modifier
                .padding(top = 9.dp, bottom = 23.dp),
            text = stringResource(id = R.string.meetings_custom_recurrence_occurs_every_section),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onPrimary
        )

        Row {
            TextFieldChip(
                onTextChange = {
                    onNumberClicked(it)
                },
                modifier = modifier
                    .padding(end = 10.dp),
                text = if (interval == -1) "" else interval.toString(),
                isDisabled = isWeekdaysSelected,
                onFocusChange = onFocusChanged
            )

            dropdownOccurrenceType?.let { dropdown ->
                DropdownMenuChip(
                    onDropdownExpanded = onFocusChanged,
                    options = listOf(
                        DropdownOccurrenceType.Day,
                        DropdownOccurrenceType.Week,
                        DropdownOccurrenceType.Month,
                    ),
                    onOptionSelected = onTypeClicked,
                    modifier = modifier,
                    initialSelectedOption = dropdown,
                    iconId = R.drawable.arrow_expand,
                    isDisabled = isWeekdaysSelected,
                    optionDescriptionMapper = { option ->
                        val value =
                            if (interval == -1 || interval == 0) 1 else interval

                        when (option) {
                            DropdownOccurrenceType.Day -> pluralStringResource(
                                DropdownOccurrenceType.Day.StringId,
                                value,
                                value
                            )

                            DropdownOccurrenceType.Week -> pluralStringResource(
                                DropdownOccurrenceType.Week.StringId,
                                value,
                                value
                            )

                            DropdownOccurrenceType.Month -> pluralStringResource(
                                DropdownOccurrenceType.Month.StringId,
                                value,
                                value
                            )
                        }
                    }
                )
            }
        }
    }
}


/**
 * Occurs daily option
 *
 * @param isWeekdaysSelected
 */
@Composable
private fun OccursDailySection(
    isWeekdaysSelected: Boolean,
    onWeekdaysClicked: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .testTag(TEST_TAG_OCCURS_DAILY)
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 23.dp, top = 0.dp, end = 16.dp)
    ) {

        TextButtonChip(
            onClick = onWeekdaysClicked,
            text = stringResource(id = R.string.meetings_custom_recurrence_weekdays_button).lowercase(),
            modifier = modifier,
            isChecked = isWeekdaysSelected,
            iconId = R.drawable.icon_check
        )
    }
}

/**
 * Custom Recurrence View Preview
 */
@CombinedThemePreviews
@Composable
private fun PreviewCustomRecurrenceView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CustomRecurrenceView(
            state = CreateScheduledMeetingState(
                meetingTitle = "Title meeting",
                rulesSelected = ChatScheduledRules(),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackBar = null
            ),
            onScrollChange = {},
            onRejectClicked = {},
            onAcceptClicked = {},
            onTypeClicked = {},
            onNumberClicked = {},
            onWeekdaysClicked = {},
            onFocusChanged = {}
        )
    }
}

internal const val TEST_TAG_OCCURS_DAILY = "testTagOccursDaily"
internal const val TEST_TAG_OCCURS_EVERY = "testTagOccursEvery"
internal const val TEST_TAG_ACCEPT_ICON = "testTagTextAcceptIcon"
internal const val TEST_TAG_BACK_ICON = "testTagBackIcon"