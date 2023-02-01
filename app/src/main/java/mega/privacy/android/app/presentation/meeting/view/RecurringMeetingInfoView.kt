package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccursType

/**
 * Recurring meeting info View
 */
@Composable
fun RecurringMeetingInfoView(
    state: RecurringMeetingInfoState,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onOccurrenceClicked: (ChatScheduledMeetingOccurr) -> Unit = {},
    onSeeMoreClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

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
            modifier = Modifier.padding(paddingValues)
        ) {

            item(key = "Occurrences list") {
                state.occurrencesList.indices.forEach { i ->
                    OccurrenceItemView(
                        occurrence = state.occurrencesList[i],
                        onOccurrenceClicked = onOccurrenceClicked
                    )
                }

                if (state.occurrencesList.size > 10) {
                    SeeMoreOccurrencesButton(
                        onSeeMoreClicked = onSeeMoreClicked
                    )
                }
            }
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)
    onScrollChange(!firstItemVisible)
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
                    Text(text = state.schedTitle,
                        style = MaterialTheme.typography.subtitle1,
                        color = black.takeIf { isLight } ?: white,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                Text(text = when (state.typeOccurs) {
                    OccursType.Daily -> stringResource(id = R.string.meetings_recurring_meeting_info_occurs_daily_subtitle)
                    OccursType.Weekly -> stringResource(id = R.string.meetings_recurring_meeting_info_occurs_weekly_subtitle)
                    OccursType.Monthly -> stringResource(id = R.string.meetings_recurring_meeting_info_occurs_monthly_subtitle)
                },
                    style = MaterialTheme.typography.subtitle2,
                    color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
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
 * @param occurrence               [ChatScheduledMeetingOccurr]
 * @param onOccurrenceClicked      Detect when a occurrence is clicked
 */
@Composable
private fun OccurrenceItemView(
    occurrence: ChatScheduledMeetingOccurr,
    onOccurrenceClicked: (ChatScheduledMeetingOccurr) -> Unit = {},
) {
    Column {
        Row(modifier = Modifier
            .clickable {
                onOccurrenceClicked(occurrence)
            }
            .fillMaxWidth()
            .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
        }
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
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_down),
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