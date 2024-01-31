package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Chat edit message view
 *
 * @param content editing message content
 * @param modifier Modifier
 */
@Composable
fun ChatEditMessageView(
    content: String,
    modifier: Modifier = Modifier,
    onCloseEditing: () -> Unit = {},
) {
    Row(modifier = modifier.height(IntrinsicSize.Max)) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(MegaTheme.colors.components.selectionControl)
        )

        Column(
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f),
        ) {
            MegaText(
                text = stringResource(id = R.string.edit_chat_message),
                style = MaterialTheme.typography.body4,
                textColor = TextColor.Accent
            )

            MegaText(
                modifier = Modifier.padding(top = 2.dp),
                text = content,
                style = MaterialTheme.typography.caption,
                textColor = TextColor.Secondary,
                overflow = LongTextBehaviour.Ellipsis(),
            )
        }

        Icon(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(16.dp).clickable(onClick = onCloseEditing),
            painter = painterResource(id = R.drawable.ic_universal_close),
            tint = MegaTheme.colors.icon.secondary,
            contentDescription = "Icon close"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatEditMessageViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatEditMessageView(content = "This is a message")
    }
}