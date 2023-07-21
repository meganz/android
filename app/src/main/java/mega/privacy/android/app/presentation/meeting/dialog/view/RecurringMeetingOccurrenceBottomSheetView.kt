package mega.privacy.android.app.presentation.meeting.dialog.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.list.view.ChatDivider
import mega.privacy.android.app.presentation.extensions.getDayAndMonth
import mega.privacy.android.app.presentation.extensions.getTimeFormatted
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import mega.privacy.android.app.presentation.meeting.view.RecurringMeetingAvatarView
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import java.time.Instant
import java.time.temporal.ChronoField
import kotlin.random.Random

/**
 * Recurring Meeting Occurrence bottom sheet view
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun RecurringMeetingOccurrenceBottomSheetView(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    meetingState: RecurringMeetingInfoState?,
    occurrence: ChatScheduledMeetingOccurr?,
    onCancelClick: () -> Unit = {},
) {
    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        scrimColor = black.copy(alpha = 0.32f),
        sheetContent = {
            BottomSheetContent(
                modalSheetState = modalSheetState,
                coroutineScope = coroutineScope,
                meetingState = meetingState,
                occurrence = occurrence,
                onCancelClick = onCancelClick
            )
        }
    ) {}
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetContent(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    meetingState: RecurringMeetingInfoState?,
    occurrence: ChatScheduledMeetingOccurr?,
    onCancelClick: () -> Unit = {},
) {
    if (meetingState == null || occurrence == null) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(71.dp)
        ) {
            val (avatarImage, titleText, subtitleText) = createRefs()
            Box(modifier = Modifier
                .size(40.dp)
                .background(Color.Transparent)
                .constrainAs(avatarImage) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }) {
                RecurringMeetingAvatarView(state = meetingState)
            }

            Text(
                text = meetingState.schedTitle.orEmpty(),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.textColorPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(titleText) {
                    linkTo(avatarImage.end, parent.end, 16.dp, 32.dp, 0.dp, 0.dp, 0f)
                    top.linkTo(avatarImage.top)
                    bottom.linkTo(subtitleText.top)
                    width = Dimension.preferredWrapContent
                }
            )

            Text(
                text = "${occurrence.getDayAndMonth()} ${occurrence.getTimeFormatted(meetingState.is24HourFormat)}",
                color = MaterialTheme.colors.textColorSecondary,
                style = MaterialTheme.typography.subtitle2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(subtitleText) {
                    linkTo(avatarImage.end, parent.end, 16.dp, 16.dp, 0.dp, 0.dp, 0f)
                    top.linkTo(titleText.bottom)
                    bottom.linkTo(avatarImage.bottom)
                    width = Dimension.wrapContent
                }
            )
        }

        ChatDivider(startPadding = 16.dp)

        MenuItem(
            modifier = Modifier.testTag("cancel_occurrence"),
            res = R.drawable.ic_trash,
            text = R.string.general_cancel,
            description = "Cancel",
            tintRed = true,
            onClick = {
                coroutineScope.launch { modalSheetState.hide() }
                onCancelClick()
            }
        )
    }
}

@Composable
private fun MenuItem(
    modifier: Modifier,
    @DrawableRes res: Int,
    @StringRes text: Int,
    description: String,
    tintRed: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .clickable(onClick = onClick)
    ) {
        val iconColor: Color
        val textColor: Color
        if (tintRed) {
            iconColor = MaterialTheme.colors.red_600_red_300
            textColor = MaterialTheme.colors.red_600_red_300
        } else {
            iconColor = MaterialTheme.colors.grey_alpha_054_white_alpha_054
            textColor = MaterialTheme.colors.textColorPrimary
        }
        Icon(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = res),
            contentDescription = description,
            tint = iconColor
        )
        Text(
            modifier = Modifier
                .padding(start = 32.dp, end = 16.dp)
                .align(Alignment.CenterVertically),
            text = stringResource(id = text),
            color = textColor,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun PreviewRecurringMeetingOccurrenceBottomSheetView() {
    val schedId = Random.nextLong()
    RecurringMeetingOccurrenceBottomSheetView(
        modalSheetState = ModalBottomSheetState(ModalBottomSheetValue.Expanded),
        coroutineScope = rememberCoroutineScope(),
        meetingState = RecurringMeetingInfoState(
            finish = false,
            chatId = Random.nextLong(),
            schedId = schedId,
            schedTitle = "Book Club - Breast&Eggs",
            schedUntil = 0L,
            typeOccurs = OccurrenceFrequencyType.Weekly,
            occurrencesList = emptyList(),
            firstParticipant = null,
            secondParticipant = null,
            showSeeMoreButton = false,
            is24HourFormat = false,
        ),
        occurrence = ChatScheduledMeetingOccurr(
            schedId = schedId,
            parentSchedId = -1,
            isCancelled = false,
            timezone = null,
            startDateTime = Instant.parse("2023-05-30T10:00:00.00Z")
                .getLong(ChronoField.INSTANT_SECONDS),
            endDateTime = Instant.parse("2023-05-30T11:00:00.00Z")
                .getLong(ChronoField.INSTANT_SECONDS),
            overrides = null,
        ),
    )
}