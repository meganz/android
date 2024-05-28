package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

internal const val TEST_TAG_FIRST_MESSAGE_HEADER_TITLE = "first_message_header_title:title"
internal const val TEST_TAG_FIRST_MESSAGE_HEADER_SUBTITLE = "first_message_header_title:subtitle"

/**
 * First message header title
 *
 * @param title
 * @param modifier
 * @param subtitle
 */
@Composable
fun FirstMessageHeaderTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.testTag(TEST_TAG_FIRST_MESSAGE_HEADER_TITLE),
            text = title,
            color = MegaOriginalTheme.colors.text.primary,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
        )
        subtitle?.let {
            Text(
                modifier = Modifier.testTag(TEST_TAG_FIRST_MESSAGE_HEADER_SUBTITLE),
                text = it,
                color = MegaOriginalTheme.colors.text.secondary,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}

internal const val TEST_TAG_FIRST_MESSAGE_SUBTITLE_WITH_ICON_TEXT =
    "first_message_subtitle_with_icon:text"
internal const val TEST_TAG_FIRST_MESSAGE_SUBTITLE_WITH_ICON =
    "first_message_subtitle_with_icon:icon"

@Composable
fun FirstMessageHeaderSubtitleWithIcon(
    subtitle: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp).testTag(TEST_TAG_FIRST_MESSAGE_SUBTITLE_WITH_ICON),
            painter = painterResource(id = iconRes),
            tint = MegaOriginalTheme.colors.icon.primary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp).testTag(
                TEST_TAG_FIRST_MESSAGE_SUBTITLE_WITH_ICON_TEXT
            ),
            text = subtitle,
            color = MegaOriginalTheme.colors.text.primary,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Normal,
        )
    }
}


@Composable
fun FirstMessageHeaderParagraph(
    paragraph: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = paragraph,
        color = MegaOriginalTheme.colors.text.secondary,
        style = MaterialTheme.typography.subtitle2,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewFirstMessageHeaderTitle() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FirstMessageHeaderTitle(
            modifier = Modifier,
            title = "Bob Club",
            subtitle = "Mon, 5 Aug 2022 from 10:00am to 11:00am",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewFirstMessageHeaderParagraph() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FirstMessageHeaderParagraph(
            modifier = Modifier,
            paragraph = "MEGA protects your communications with our zero-knowledge encryption system providing essential safety assurances:",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewFirstMessageHeaderSubtitleWithIcon() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FirstMessageHeaderSubtitleWithIcon(
            modifier = Modifier,
            subtitle = "Confidentiality",
            iconRes = R.drawable.ic_info
        )
    }
}

