package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

@Composable
internal fun ChatOptions() = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(start = 48.dp, top = 4.dp, bottom = 8.dp, end = 48.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chat_outline),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary
            )
        }
        Text(
            text = stringResource(id = R.string.message_button),
            style = MaterialTheme.typography.caption.copy(MaterialTheme.colors.textColorPrimary),
            textAlign = TextAlign.Center,
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { /*TODO*/ }, modifier = Modifier.padding(0.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_phone),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary
            )
        }
        Text(
            text = stringResource(id = R.string.call_button),
            style = MaterialTheme.typography.caption.copy(MaterialTheme.colors.textColorPrimary),
            textAlign = TextAlign.Center,
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_video_outline),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary
            )
        }
        Text(
            text = stringResource(id = R.string.video_button),
            style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.textColorPrimary),
            textAlign = TextAlign.Center,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewChatOptionsLight() {
    AndroidTheme(isDark = false) {
        Surface {
            ChatOptions()
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewChatOptionsDark() {
    AndroidTheme(isDark = true) {
        Surface {
            ChatOptions()
        }
    }
}