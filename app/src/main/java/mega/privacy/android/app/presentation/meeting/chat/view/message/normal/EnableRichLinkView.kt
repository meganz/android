package mega.privacy.android.app.presentation.meeting.chat.view.message.normal

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Enable rich link view
 *
 * @param alwaysAllowClick Always allow click
 * @param notAllowClick Not allow click
 * @param neverClick Never click after show confirmation
 * @param denyNeverClick Deny never click after show confirmation
 * @param isShowNeverButton Is show never button
 */
@Composable
fun EnableRichLinkView(
    alwaysAllowClick: () -> Unit,
    notAllowClick: () -> Unit,
    neverClick: () -> Unit,
    denyNeverClick: () -> Unit,
    isShowNeverButton: Boolean,
) {
    var showConfirmationDisableRichLink by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.End) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_rich_link),
                contentDescription = "Rich Link Image"
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MegaText(
                    text = stringResource(id = R.string.title_enable_rich_links),
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1,
                )
                MegaText(
                    text = stringResource(id = R.string.text_enable_rich_links),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.body4,
                )
            }
        }
        if (showConfirmationDisableRichLink) {
            Row(Modifier.padding(top = 8.dp)) {
                TextMegaButton(
                    text = stringResource(id = R.string.general_yes),
                    onClick = neverClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                )
                TextMegaButton(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.general_no),
                    onClick = denyNeverClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        } else {
            TextMegaButton(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(id = R.string.button_always_rich_links),
                onClick = alwaysAllowClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            )
            TextMegaButton(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(id = R.string.button_always_rich_links),
                onClick = notAllowClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            )
            if (isShowNeverButton) {
                TextMegaButton(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(id = R.string.button_never_rich_links),
                    onClick = { showConfirmationDisableRichLink = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun EnableRichLinkViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        EnableRichLinkView(
            alwaysAllowClick = {},
            notAllowClick = {},
            neverClick = {},
            denyNeverClick = {},
            isShowNeverButton = true,
        )
    }
}