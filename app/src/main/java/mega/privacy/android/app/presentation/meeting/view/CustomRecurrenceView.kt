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
import mega.privacy.android.app.presentation.extensions.meeting.StringId
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.core.ui.controls.chips.DropdownMenuChip
import mega.privacy.android.core.ui.controls.chips.TextButtonChip
import mega.privacy.android.core.ui.controls.chips.TextFieldChip
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType

/**
 * Custom recurrence View
 */
@Composable
internal fun CustomRecurrenceView(
    state: CustomRecurrenceState,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onAcceptClicked: () -> Unit,
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
                state = state,
                onAcceptClicked = onAcceptClicked,
                onBackPressed = onBackPressed,
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
                    state = state,
                    onTypeClicked = onTypeClicked,
                    onNumberClicked = onNumberClicked,
                    onFocusChanged = onFocusChanged
                )
            }
            if (state.dropdownOccurrenceType == DropdownOccurrenceType.Day) {
                item(key = "Occurs daily") {
                    OccursDailySection(
                        modifier = Modifier,
                        isWeekdaysSelected = state.isWeekdaysSelected,
                        onWeekdaysClicked = onWeekdaysClicked,
                    )
                }
            }
        }
    }

    onScrollChange(!firstItemVisible)
}

/**
 * Occurs every option
 *
 * @param state [CustomRecurrenceState]
 *
 */
@Composable
private fun OccursEverySection(
    state: CustomRecurrenceState,
    onNumberClicked: (String) -> Unit,
    onTypeClicked: (DropdownOccurrenceType) -> Unit,
    onFocusChanged: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
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
                text = if (state.rules.interval == -1) "" else state.rules.interval.toString(),
                isDisabled = state.isWeekdaysSelected,
                onFocusChange = onFocusChanged
            )

            DropdownMenuChip(
                onDropdownExpanded = onFocusChanged,
                options = listOf(
                    DropdownOccurrenceType.Day,
                    DropdownOccurrenceType.Week,
                    DropdownOccurrenceType.Month,
                ),
                onOptionSelected = onTypeClicked,
                modifier = modifier,
                initialSelectedOption = state.dropdownOccurrenceType,
                iconId = R.drawable.arrow_expand,
                isDisabled = state.isWeekdaysSelected,
                optionDescriptionMapper = { option ->
                    val value =
                        if (state.rules.interval == -1 || state.rules.interval == 0) 1 else state.rules.interval
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

/**
 * Occurs daily option
 *
 * @param isWeekdaysSelected
 *
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
 * Custom recurrence App bar view
 *
 * @param state                     [CustomRecurrenceState]
 * @param onAcceptClicked           When on accept recurrence is clicked
 * @param onBackPressed             When on back pressed
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun CustomRecurrenceAppBar(
    state: CustomRecurrenceState,
    onAcceptClicked: () -> Unit,
    onBackPressed: () -> Unit,
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
            IconButton(modifier = Modifier.testTag(TEST_TAG_BACK_ICON), onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        },
        actions = {
            IconButton(modifier = Modifier.testTag(TEST_TAG_ACCEPT_ICON), onClick = {
                if (state.isValidRecurrence) {
                    onAcceptClicked()
                }
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_confirm),
                    contentDescription = "Accept custom recurrence button",
                    tint = if (state.isValidRecurrence) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.grey_alpha_038_white_alpha_038
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

/**
 * Custom Recurrence View Preview
 */
@CombinedThemePreviews
@Composable
private fun PreviewCustomRecurrenceView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CustomRecurrenceView(
            state = CustomRecurrenceState(
                rules = ChatScheduledRules(
                    freq = OccurrenceFrequencyType.Daily,
                    interval = 1,
                    until = 0,
                    weekDayList = null,
                    monthDayList = null,
                    monthWeekDayList = emptyList()
                ),
                dropdownOccurrenceType = DropdownOccurrenceType.Day,
                maxOccurrenceNumber = 99,
                isWeekdaysSelected = false,
                isValidRecurrence = false
            ),
            onAcceptClicked = {},
            onScrollChange = {},
            onBackPressed = {},
            onTypeClicked = {},
            onNumberClicked = {},
            onWeekdaysClicked = {},
            onFocusChanged = {}
        )
    }
}

internal const val TEST_TAG_OCCURS_DAILY = "testTagOccursDaily"
internal const val TEST_TAG_ACCEPT_ICON = "testTagTextAcceptIcon"
internal const val TEST_TAG_BACK_ICON = "testTagBackIcon"
