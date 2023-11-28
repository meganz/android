package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

@Composable
fun ChatBubble(
    isMe: Boolean,
    modifier: Modifier = Modifier,
    subContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = if (isMe) MegaTheme.colors.button.primary else MegaTheme.colors.background.surface2,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column {
            CompositionLocalProvider(
                LocalContentColor provides if (isMe) MegaTheme.colors.text.inverse else MegaTheme.colors.text.primary,
                LocalTextStyle provides MaterialTheme.typography.subtitle2,
            ) {
                content()
            }
            CompositionLocalProvider(
                LocalContentColor provides MegaTheme.colors.text.primary,
            ) {
                Box(
                    modifier = Modifier
                        .padding(1.dp)
                        .background(
                            color = MegaTheme.colors.background.pageBackground,
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        ),
                ) {
                    subContent()
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChatBubblePreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = isMe) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = "Hello world!"
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChatBubbleWithSubContentPreview(
    @PreviewParameter(BooleanProvider::class) isMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = isMe, subContent = {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = "Second Text"
            )
        }) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = "Hello world!"
            )
        }
    }
}