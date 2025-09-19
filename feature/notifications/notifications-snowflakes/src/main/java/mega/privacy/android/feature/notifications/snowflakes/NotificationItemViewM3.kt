package mega.privacy.android.feature.notifications.snowflakes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.DefaultChipStyle
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun NotificationItemViewM3(
    titleColor: TextColor,
    typeTitle: String,
    title: String,
    description: String?,
    subText: AnnotatedString?,
    date: String,
    isNew: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .background(
                color = if (isNew) DSTokens.colors.background.pageBackground
                else DSTokens.colors.background.surface1
            )
            .testTag(NOTIFICATION_ITEM_VIEW_M3_TEST_TAG)
            .fillMaxWidth()
            .wrapContentHeight()) {

        MegaText(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .testTag(NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG),
            textColor = titleColor,
            text = typeTitle,
            style = AppTheme.typography.labelSmall.copy(fontWeight = SemiBold),
            overflow = TextOverflow.Ellipsis
        )

        NotificationTitleRow(
            title = title,
            showNewIcon = isNew,
            reducedLines = !description.isNullOrBlank()
        )

        if (!description.isNullOrBlank()) {
            LinkSpannedText(
                value = description,
                baseStyle = AppTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                spanStyles = spanStyles,
                onAnnotationClick = {},
                baseTextColor = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 3.dp)
                    .testTag(NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG)
            )
        }

        subText?.let {
            MegaText(
                text = it.toString(),
                modifier = Modifier
                    .padding(start = 16.dp, top = 2.dp, end = 16.dp)
                    .testTag(NOTIFICATION_ITEM_VIEW_SUB_TEXT_M3_TEST_TAG),
                style = AppTheme.typography.labelSmall,
                textColor = TextColor.Secondary,
                maxLines = 2,
            )
        }
        MegaText(
            text = date,
            textColor = TextColor.Secondary,
            overflow = TextOverflow.Clip,
            style = AppTheme.typography.labelSmall,
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
                .testTag(NOTIFICATION_ITEM_VIEW_DATE_M3_TEST_TAG)
        )
        val dividerModifier = modifier
            .fillMaxWidth()

        if (isNew) SubtleDivider(
            dividerModifier.testTag(
                NOTIFICATION_ITEM_VIEW_SUBTLE_DIVIDER_M3_TEST_TAG
            )
        ) else StrongDivider(
            dividerModifier.testTag(NOTIFICATION_ITEM_VIEW_STRONG_DIVIDER_M3_TEST_TAG)
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
            .testTag(NOTIFICATION_ITEM_VIEW_TITLE_ROW_M3_TEST_TAG)
            .padding(start = 16.dp, end = 16.dp)
    ) {
        LinkSpannedText(
            value = title,
            baseStyle = AppTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
            spanStyles = spanStyles,
            onAnnotationClick = {},
            baseTextColor = TextColor.Primary,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .weight(1f)
                .testTag(NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG)
        )

        if (showNewIcon) {
            MegaChip(
                selected = true,
                text = stringResource(sharedR.string.notifications_notification_item_new_tag),
                style = DefaultChipStyle,
                modifier = Modifier.testTag(NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG)
            )
        }
    }
}

private val spanStyles = hashMapOf(
    SpanIndicator('A') to SpanStyleWithAnnotation(
        MegaSpanStyle.TextColorStyle(
            SpanStyle(),
            TextColor.Primary
        ), null
    ),
    SpanIndicator('B') to SpanStyleWithAnnotation(
        MegaSpanStyle.TextColorStyle(
            SpanStyle(),
            TextColor.Secondary
        ), null
    )
)

@CombinedThemePreviews
@Composable
private fun NotificationItemViewM3Preview() {
    val sampleSubText = buildAnnotatedString {
        withStyle(style = SpanStyle()) {
            append("Meeting scheduled for ")
        }
        withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("tomorrow at 2:00 PM")
        }
    }

    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NotificationItemViewM3(
            titleColor = TextColor.Error,
            typeTitle = "CONTACTS",
            title = "New Contact Request",
            description = "John Doe wants to connect with you. [A]This is a longer description[/A] that might wrap to multiple lines. [B]This part should be styled[/B] differently.",
            subText = sampleSubText,
            date = "11 October 2022 6:46 pm",
            isNew = true,
            onClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NotificationItemViewM3WithoutDescriptionPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NotificationItemViewM3(
            titleColor = TextColor.Warning,
            typeTitle = "INCOMING SHARES",
            title = "File Shared with You",
            description = null,
            subText = null,
            date = "10 October 2022 3:30 pm",
            isNew = false,
            onClick = {}
        )
    }
}

internal const val NOTIFICATION_ITEM_VIEW_STRONG_DIVIDER_M3_TEST_TAG =
    "notification_item_view:strong_divider"
internal const val NOTIFICATION_ITEM_VIEW_SUBTLE_DIVIDER_M3_TEST_TAG =
    "notification_item_view:subtle_divider"
internal const val NOTIFICATION_ITEM_VIEW_DESCRIPTION_M3_TEST_TAG =
    "notification_item_view:description_text"
internal const val NOTIFICATION_ITEM_VIEW_TITLE_ROW_TITLE_M3_TEST_TAG =
    "notification_item_view:title_text"
internal const val NOTIFICATION_ITEM_VIEW_TITLE_ROW_M3_TEST_TAG = "notification_item_view:title_row"
internal const val NOTIFICATION_ITEM_VIEW_SECTION_TITLE_M3_TEST_TAG =
    "notification_item_view:section_title"
const val NOTIFICATION_ITEM_VIEW_M3_TEST_TAG = "notification_item_view"
internal const val NOTIFICATION_ITEM_VIEW_DATE_M3_TEST_TAG =
    "notification_item_view:date_text"
const val NOTIFICATION_ITEM_VIEW_NEW_M3_TEST_TAG =
    "notification_item_view:new_text"
const val NOTIFICATION_ITEM_VIEW_SUB_TEXT_M3_TEST_TAG = "notification_item_view:sub_text"
