package mega.privacy.android.core.ui.controls.chat

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme


@Composable
fun FirstMessageHeaderTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = MegaTheme.colors.text.primary,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Medium,
        )
        subtitle?.let {
            Text(
                text = it,
                color = MegaTheme.colors.text.secondary,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}

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
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = iconRes),
            tint = MegaTheme.colors.icon.primary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = subtitle,
            color = MegaTheme.colors.text.primary,
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
        color = MegaTheme.colors.text.secondary,
        style = MaterialTheme.typography.subtitle2,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewFirstMessageHeaderTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FirstMessageHeaderParagraph(
            modifier = Modifier,
            paragraph = "MEGA protects your communications with our zero-knowledge encryption system providing essential safety assurances:",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewFirstMessageHeaderSubtitleWithIcon() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FirstMessageHeaderSubtitleWithIcon(
            modifier = Modifier,
            subtitle = "Confidentiality",
            iconRes = R.drawable.ic_info
        )
    }
}

