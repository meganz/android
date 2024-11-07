package mega.privacy.android.shared.original.core.ui.controls.notifications

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.TagChipStyle
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Type of the notification t
 */
enum class NotificationItemType {
    Contacts,
    IncomingShares,
    ScheduledMeetings,
    Custom,
    Others;

    @Composable
    internal fun titleColor() = when (this) {
        ScheduledMeetings -> TextColor.Error
        IncomingShares -> TextColor.Warning
        Contacts -> TextColor.Accent
        Custom -> TextColor.Error
        Others -> TextColor.Warning
    }
}

@Composable
fun NotificationItemView(
    type: NotificationItemType,
    typeTitle: String,
    title: String,
    description: String?,
    subText: AnnotatedString?,
    date: String,
    isNew: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier = modifier
        .clickable { onClick() }
        .background(
            color = if (isNew) MegaOriginalTheme.colors.background.pageBackground
            else MegaOriginalTheme.colors.background.surface1
        )
        .testTag(NOTIFICATION_TEST_TAG)
        .fillMaxWidth()
        .wrapContentHeight()) {

        MegaText(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .testTag(NOTIFICATION_SECTION_TITLE_TEST_TAG),
            textColor = type.titleColor(),
            text = typeTitle,
            style = MaterialTheme.typography.caption.copy(fontWeight = SemiBold),
            overflow = LongTextBehaviour.Ellipsis(1)
        )


        NotificationTitleRow(
            title = title,
            showNewIcon = isNew,
            reducedLines = !description.isNullOrBlank()
        )

        if (!description.isNullOrBlank()) {
            MegaSpannedText(
                value = description,
                baseStyle = MaterialTheme.typography.subtitle2.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                styles = spanStyles,
                color = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 3.dp)
                    .testTag(NOTIFICATION_DESCRIPTION_TEST_TAG)
            )
        }

        subText?.let {
            MegaText(
                text = it,
                modifier = Modifier
                    .padding(start = 16.dp, top = 2.dp, end = 16.dp)
                    .testTag("SchedMeetingTime"),
                style = MaterialTheme.typography.caption,
                textColor = TextColor.Secondary,
                maxLines = 2,
            )
        }
        MegaText(
            text = date,
            textColor = TextColor.Secondary,
            overflow = LongTextBehaviour.Clip(),
            style = MaterialTheme.typography.caption,
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
                .testTag(NOTIFICATION_DATE_TEST_TAG)
        )
        MegaDivider(
            modifier = Modifier.testTag(NOTIFICATION_DIVIDER),
            dividerType = DividerType.FullSize,
        )
    }
}


@Composable
private fun NotificationTitleRow(
    title: String,
    showNewIcon: Boolean,
    reducedLines: Boolean,
) {
    val titleMaxLines = if (reducedLines) 1 else 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NOTIFICATION_TITLE_ROW_TEST_TAG)
            .padding(start = 16.dp, end = 16.dp)
    ) {
        MegaSpannedText(
            value = title,
            baseStyle = MaterialTheme.typography.subtitle1.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
            styles = spanStyles,
            color = TextColor.Primary,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .weight(1f)
                .testTag(NOTIFICATION_TITLE_ROW_TITLE_TEST_TAG)
        )

        if (showNewIcon) {
            MegaChip(
                selected = true,
                text = stringResource(sharedR.string.notifications_notification_item_new_tag),
                style = TagChipStyle,
                modifier = Modifier.testTag(NOTIFICATION_GREEN_ICON_TEST_TAG)
            )
        }
    }
}

private val spanStyles = mapOf(
    SpanIndicator('A') to MegaSpanStyle(color = TextColor.Primary),
    SpanIndicator('B') to MegaSpanStyle(color = TextColor.Secondary)
)

/**
 * helper class to help passing all the parameters to tests and previews.
 */
internal data class NotificationItemDefinition(
    val type: NotificationItemType,
    val typeTitle: String,
    val title: String,
    val description: String?,
    val subText: AnnotatedString?,
    val date: String,
    val isNew: Boolean,
)

private class NotificationProvider : PreviewParameterProvider<NotificationItemDefinition> {
    override val values = NotificationItemType.entries
        .flatMap { listOf(it to true, it to false) }.map { (section, isNew) ->
            NotificationItemDefinition(
                type = section,
                typeTitle = "Meetings",
                title = "Title",
                description = "name@email.com [B]meeting text meeting text meeting text[/B] hi",
                subText = buildAnnotatedString {
                    append("Every ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Monday")
                    }
                    append(" at ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("11 am")
                    }
                },
                date = "11 October 2022 6:46 pm",
                isNew = isNew,
            )
        }.asSequence()
}

@CombinedTextAndThemePreviews
@Composable
private fun NotificationItemViewPreview(
    @PreviewParameter(NotificationProvider::class) notification: NotificationItemDefinition,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NotificationItemView(
            notification.type,
            notification.typeTitle,
            notification.title,
            notification.description,
            notification.subText,
            notification.date,
            notification.isNew,
        ){}
    }
}

internal const val NOTIFICATION_DIVIDER = "notification_item_view:mega_divider"
internal const val NOTIFICATION_DESCRIPTION_TEST_TAG = "notification_item_view:description_text"
internal const val NOTIFICATION_TITLE_ROW_TITLE_TEST_TAG = "notification_title_row:title_text"
internal const val NOTIFICATION_TITLE_ROW_TEST_TAG = "notification_title_row"
internal const val NOTIFICATION_SECTION_TITLE_TEST_TAG =
    "notification_item_view:section_title"
const val NOTIFICATION_TEST_TAG = "notification_item_view"
internal const val NOTIFICATION_DATE_TEST_TAG =
    "notification_item_view:notification_date:date_text"
const val NOTIFICATION_GREEN_ICON_TEST_TAG =
    "notification_item_view:notification_title_row:new_text"
