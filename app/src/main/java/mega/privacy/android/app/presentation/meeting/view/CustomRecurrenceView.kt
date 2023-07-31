package mega.privacy.android.app.presentation.meeting.view

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.palm.composestateevents.consumed
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.meeting.DropdownType
import mega.privacy.android.app.presentation.extensions.meeting.InitialLetterStringId
import mega.privacy.android.app.presentation.extensions.meeting.StringId
import mega.privacy.android.app.presentation.meeting.model.CreateScheduledMeetingState
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.core.ui.controls.chips.DropdownMenuChip
import mega.privacy.android.core.ui.controls.chips.TextButtonChip
import mega.privacy.android.core.ui.controls.chips.TextButtonWithIconChip
import mega.privacy.android.core.ui.controls.chips.TextFieldChip
import mega.privacy.android.core.ui.controls.divider.CustomDivider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.EndsRecurrenceOption
import mega.privacy.android.domain.entity.meeting.MonthlyRecurrenceOption
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import java.time.ZonedDateTime

/**
 * Custom recurrence View
 */
@Composable
internal fun CustomRecurrenceView(
    state: CreateScheduledMeetingState,
    onAcceptClicked: () -> Unit,
    onRejectClicked: () -> Unit,
    onWeekdaysClicked: () -> Unit,
    onFocusChanged: () -> Unit,
    onDateClicked: () -> Unit,
    onScrollChange: (Boolean) -> Unit,
    onIntervalChanged: (String) -> Unit,
    onMonthDayChanged: (String) -> Unit,
    onMonthWeekDayChanged: (Weekday) -> Unit,
    onFrequencyTypeChanged: (DropdownOccurrenceType) -> Unit,
    onDayClicked: (Weekday) -> Unit,
    onMonthlyRadioButtonClicked: (MonthlyRecurrenceOption) -> Unit,
    onEndsRadioButtonClicked: (EndsRecurrenceOption) -> Unit,
    onWeekOfMonthChanged: (WeekOfMonth) -> Unit,
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
        BackPressHandler(onBackPressed = onRejectClicked)

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
                    onFrequencyTypeChanged = onFrequencyTypeChanged,
                    onIntervalChanged = onIntervalChanged,
                    onFocusChanged = onFocusChanged
                )
            }

            when (state.customRecurrenceState.newRules.freq.DropdownType) {
                DropdownOccurrenceType.Day ->
                    item(key = "Occurs daily") {
                        OccursDailySection(
                            modifier = Modifier,
                            isWeekdaysSelected = state.customRecurrenceState.isWeekdaysSelected,
                            onWeekdaysClicked = onWeekdaysClicked,
                        )
                    }

                DropdownOccurrenceType.Week ->
                    item(key = "Occurs weekly") {
                        OccursWeeklySection(
                            modifier = Modifier,
                            weekList = state.weekList,
                            customRecurrenceList = state.customRecurrenceState.newRules.weekDayList
                                ?: emptyList(),
                            onDayClicked = onDayClicked,
                        )
                    }

                DropdownOccurrenceType.Month ->
                    item(key = "Occurs monthly") {
                        val monthDayList = state.customRecurrenceState.monthDayOption
                        val monthWeekDayList = state.customRecurrenceState.monthWeekDayListOption

                        OccursMonthlySection(
                            modifier = Modifier,
                            monthlyOptionSelected = state.customRecurrenceState.monthlyRadioButtonOptionSelected,
                            showWarning = state.customRecurrenceState.showMonthlyRecurrenceWarning,
                            dropdownWeekdaySelected = monthWeekDayList.first().weekDaysList.first(),
                            dropdownWeekOfMonthSelected = monthWeekDayList.first().weekOfMonth,
                            monthDayOptionSelected = when (monthDayList) {
                                -1 -> ""
                                else -> monthDayList.toString()
                            },
                            onRadioButtonClicked = onMonthlyRadioButtonClicked,
                            onMonthDayChanged = onMonthDayChanged,
                            onWeekdayChanged = onMonthWeekDayChanged,
                            onWeekOfMonthChanged = onWeekOfMonthChanged
                        )
                    }
            }

            item(key = "Ends") {
                EndsSection(
                    modifier = Modifier,
                    endsOptionSelected = state.customRecurrenceState.endsRadioButtonOptionSelected,
                    onRadioButtonClicked = onEndsRadioButtonClicked,
                    date = state.customRecurrenceState.endDateOccurrenceOption,
                    onDateClicked = onDateClicked,
                )
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

@Composable
private fun BackPressHandler(
    backPressedDispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher,
    onBackPressed: () -> Unit,
) {
    val currentOnBackPressed by rememberUpdatedState(newValue = onBackPressed)

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentOnBackPressed()
            }
        }
    }

    DisposableEffect(key1 = backPressedDispatcher) {
        backPressedDispatcher?.addCallback(backCallback)

        onDispose {
            backCallback.remove()
        }
    }
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
    onIntervalChanged: (String) -> Unit,
    onFrequencyTypeChanged: (DropdownOccurrenceType) -> Unit,
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
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            ),
        )

        Row {
            TextFieldChip(
                onTextChange = onIntervalChanged,
                modifier = modifier
                    .padding(end = 5.dp),
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
                    onOptionSelected = onFrequencyTypeChanged,
                    modifier = modifier,
                    initialSelectedOption = dropdown,
                    iconId = R.drawable.arrow_expand,
                    isDisabled = isWeekdaysSelected,
                    optionDescriptionMapper = { option ->
                        val value =
                            if (interval == -1 || interval == 0) 1 else interval
                        pluralStringResource(
                            option.StringId,
                            value,
                            value
                        )
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
    modifier: Modifier,
    isWeekdaysSelected: Boolean,
    onWeekdaysClicked: () -> Unit,
) {
    Column(
        modifier
            .testTag(TEST_TAG_OCCURS_DAILY)
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 23.dp, top = 0.dp, end = 16.dp)
    ) {
        TextButtonWithIconChip(
            onClick = onWeekdaysClicked,
            text = stringResource(id = R.string.meetings_custom_recurrence_weekdays_button).lowercase(),
            modifier = modifier,
            isChecked = isWeekdaysSelected,
            iconId = R.drawable.icon_check
        )
    }
}

/**
 * Occurs weekly option
 * @param weekList              [Weekday] list
 * @param customRecurrenceList  [Weekday] list
 */
@Composable
private fun OccursWeeklySection(
    modifier: Modifier,
    weekList: List<Weekday>,
    customRecurrenceList: List<Weekday>,
    onDayClicked: (Weekday) -> Unit,
) {
    Column(
        modifier
            .testTag(TEST_TAG_OCCURS_WEEKLY)
            .fillMaxWidth()
            .padding(bottom = 23.dp)
    ) {
        Text(
            modifier = modifier
                .padding(top = 9.dp, bottom = 23.dp, start = 16.dp),
            text = stringResource(id = R.string.meetings_custom_recurrence_occurs_on_section),
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            ),
        )

        Row {
            weekList.forEach {
                TextButtonChip(
                    onClick = { onDayClicked(it) },
                    text = stringResource(id = it.InitialLetterStringId),
                    modifier = modifier.padding(start = 16.dp),
                    isChecked = customRecurrenceList.contains(it)
                )
            }
        }
    }
}

/**
 * Occurs monthly option
 *
 * @param monthlyOptionSelected  [MonthlyRecurrenceOption]
 * @param monthDayOptionSelected
 */
@Composable
private fun OccursMonthlySection(
    modifier: Modifier,
    monthlyOptionSelected: MonthlyRecurrenceOption,
    dropdownWeekdaySelected: Weekday,
    dropdownWeekOfMonthSelected: WeekOfMonth,
    monthDayOptionSelected: String,
    showWarning: Boolean,
    onRadioButtonClicked: (MonthlyRecurrenceOption) -> Unit,
    onMonthDayChanged: (String) -> Unit,
    onWeekOfMonthChanged: (WeekOfMonth) -> Unit,
    onWeekdayChanged: (Weekday) -> Unit,
) {
    val monthlySectionRadioOptions: List<MonthlyRecurrenceOption> = listOf(
        MonthlyRecurrenceOption.MonthDay,
        MonthlyRecurrenceOption.MonthWeekday,
    )

    Column(
        modifier
            .testTag(TEST_TAG_OCCURS_MONTHLY)
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 23.dp)
    ) {
        Text(
            modifier = modifier
                .padding(top = 9.dp),
            text = stringResource(id = R.string.meetings_custom_recurrence_occurs_on_section),
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            ),
        )

        Row {
            Column(modifier = modifier.selectableGroup()) {
                monthlySectionRadioOptions.forEach { item ->
                    Row(
                        modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .selectable(
                                selected = (item == monthlyOptionSelected),
                                onClick = {
                                    if (item != monthlyOptionSelected) {
                                        onRadioButtonClicked(item)
                                    }
                                },
                                role = Role.RadioButton
                            )
                    ) {
                        Box(
                            modifier = modifier
                                .height(56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            RadioButton(
                                selected = (item == monthlyOptionSelected),
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colors.secondary,
                                    unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    disabledColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                                ),
                                onClick = {
                                    if (item != monthlyOptionSelected) {
                                        onRadioButtonClicked(item)
                                    }
                                }
                            )
                        }
                        Box(
                            modifier = modifier
                                .height(56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (item) {
                                MonthlyRecurrenceOption.MonthDay -> {
                                    Row {
                                        Box(
                                            modifier = modifier
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.meetings_custom_recurrence_occurs_on_day_section),
                                                style = MaterialTheme.typography.subtitle1.copy(
                                                    color = MaterialTheme.colors.onPrimary,
                                                    textAlign = TextAlign.Center
                                                ),
                                            )
                                        }
                                        Box(
                                            modifier = modifier
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            TextFieldChip(
                                                onTextChange = onMonthDayChanged,
                                                modifier = modifier.padding(start = 5.dp),
                                                text = monthDayOptionSelected,
                                                isDisabled = item != monthlyOptionSelected,
                                                onFocusChange = {
                                                    if (item != monthlyOptionSelected) {
                                                        onRadioButtonClicked(item)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                MonthlyRecurrenceOption.MonthWeekday -> {
                                    Row {
                                        Box(
                                            modifier = modifier
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            DropdownMenuChip(
                                                onDropdownExpanded = {
                                                    if (item != monthlyOptionSelected) {
                                                        onRadioButtonClicked(item)
                                                    }
                                                },
                                                options = listOf(
                                                    WeekOfMonth.First,
                                                    WeekOfMonth.Second,
                                                    WeekOfMonth.Third,
                                                    WeekOfMonth.Fourth,
                                                    WeekOfMonth.Fifth,
                                                ),
                                                onOptionSelected = onWeekOfMonthChanged,
                                                modifier = modifier,
                                                initialSelectedOption = dropdownWeekOfMonthSelected,
                                                iconId = R.drawable.arrow_expand,
                                                isDisabled = item != monthlyOptionSelected,
                                                optionDescriptionMapper = { option ->
                                                    stringResource(option.StringId)
                                                }
                                            )
                                        }
                                        Box(
                                            modifier = modifier
                                                .fillMaxHeight()
                                                .padding(start = 5.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            DropdownMenuChip(
                                                onDropdownExpanded = {
                                                    if (item != monthlyOptionSelected) {
                                                        onRadioButtonClicked(item)
                                                    }
                                                },
                                                options = listOf(
                                                    Weekday.Monday,
                                                    Weekday.Tuesday,
                                                    Weekday.Wednesday,
                                                    Weekday.Thursday,
                                                    Weekday.Friday,
                                                    Weekday.Saturday,
                                                    Weekday.Sunday,
                                                ),
                                                onOptionSelected = onWeekdayChanged,
                                                modifier = modifier,
                                                initialSelectedOption = dropdownWeekdaySelected,
                                                iconId = R.drawable.arrow_expand,
                                                isDisabled = item != monthlyOptionSelected,
                                                optionDescriptionMapper = { option ->
                                                    stringResource(option.StringId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    CustomDivider(
                        withStartPadding = true, startPadding = 54.dp
                    )
                    if (item == MonthlyRecurrenceOption.MonthDay && showWarning) {
                        Row {
                            Box(
                                modifier = modifier
                                    .padding(start = 54.dp, end = 8.dp, bottom = 5.dp, top = 5.dp),
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        id = R.plurals.meetings_schedule_meeting_recurrence_monthly_description,
                                        monthDayOptionSelected.toInt(),
                                        monthDayOptionSelected.toInt()
                                    ),
                                    style = MaterialTheme.typography.subtitle2.copy(
                                        color = MaterialTheme.colors.textColorSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ends recurrence option
 *
 * @param endsOptionSelected    [EndsRecurrenceOption]
 * @param date                  [ZonedDateTime]
 */
@Composable
private fun EndsSection(
    modifier: Modifier,
    date: ZonedDateTime,
    endsOptionSelected: EndsRecurrenceOption,
    onDateClicked: () -> Unit,
    onRadioButtonClicked: (EndsRecurrenceOption) -> Unit,
) {
    val endsSectionRadioOptions: List<EndsRecurrenceOption> = listOf(
        EndsRecurrenceOption.Never,
        EndsRecurrenceOption.CustomDate,
    )

    Column(
        modifier
            .testTag(TEST_TAG_ENDS)
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 23.dp)
    ) {
        Text(
            modifier = modifier
                .padding(top = 9.dp),
            text = stringResource(id = R.string.meetings_custom_recurrence_ends_section),
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center
            ),
        )

        Row {
            Column(modifier = modifier.selectableGroup()) {
                endsSectionRadioOptions.forEach { item ->
                    Row(
                        modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .selectable(
                                selected = (item == endsOptionSelected),
                                onClick = {
                                    if (item != endsOptionSelected) {
                                        onRadioButtonClicked(item)
                                    }
                                },
                                role = Role.RadioButton
                            )
                    ) {
                        Box(
                            modifier = modifier
                                .height(56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            RadioButton(
                                selected = (item == endsOptionSelected),
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colors.secondary,
                                    unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                    disabledColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                                ),
                                onClick = {
                                    if (item != endsOptionSelected) {
                                        onRadioButtonClicked(item)
                                    }
                                }
                            )
                        }
                        Box(
                            modifier = modifier
                                .height(56.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            when (item) {
                                EndsRecurrenceOption.Never -> {
                                    Row {
                                        Box(
                                            modifier = modifier
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.meetings_schedule_meeting_recurrence_never_label),
                                                style = MaterialTheme.typography.subtitle1.copy(
                                                    color = MaterialTheme.colors.onPrimary,
                                                    textAlign = TextAlign.Center
                                                ),
                                            )
                                        }
                                    }
                                }

                                EndsRecurrenceOption.CustomDate -> {
                                    Row {
                                        Box(
                                            modifier = modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            TextFieldChip(
                                                onTextChange = {},
                                                modifier = modifier.clickable {
                                                    onDateClicked()
                                                },
                                                text = getScheduledMeetingEndRecurrenceText(date),
                                                isDisabled = item != endsOptionSelected,
                                                readOnly = true,
                                                isSmall = false,
                                                onFocusChange = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    CustomDivider(
                        withStartPadding = true, startPadding = 54.dp
                    )
                }
            }
        }
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
                customRecurrenceState = CustomRecurrenceState(),
                participantItemList = emptyList(),
                buttons = ScheduleMeetingAction.values().asList(),
                snackbarMessageContent = consumed()
            ),
            onScrollChange = {},
            onAcceptClicked = {},
            onRejectClicked = {},
            onIntervalChanged = {},
            onFrequencyTypeChanged = {},
            onDayClicked = {},
            onWeekdaysClicked = {},
            onFocusChanged = {},
            onMonthlyRadioButtonClicked = {},
            onEndsRadioButtonClicked = {},
            onMonthDayChanged = {},
            onMonthWeekDayChanged = {},
            onWeekOfMonthChanged = {},
            onDateClicked = {},
        )
    }
}

internal const val TEST_TAG_ENDS = "testTagOccursEnds"
internal const val TEST_TAG_OCCURS_MONTHLY = "testTagOccursMonthly"
internal const val TEST_TAG_OCCURS_WEEKLY = "testTagOccursWeekly"
internal const val TEST_TAG_OCCURS_DAILY = "testTagOccursDaily"
internal const val TEST_TAG_OCCURS_EVERY = "testTagOccursEvery"
internal const val TEST_TAG_ACCEPT_ICON = "testTagTextAcceptIcon"
internal const val TEST_TAG_BACK_ICON = "testTagBackIcon"