package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Chat bubble
 *
 * @param isMe Whether the message is sent by me
 * @param modifier Modifier
 * @param subContent Sub content
 * @param content Content
 */
@Composable
fun ChatBubble(
    isMe: Boolean,
    modifier: Modifier = Modifier,
    subContent: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(modifier)
            .background(
                color = if (isMe) MegaOriginalTheme.colors.button.primary else MegaOriginalTheme.colors.background.surface2,
            )
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            CompositionLocalProvider(
                LocalContentColor provides if (isMe) MegaOriginalTheme.colors.text.inverse else MegaOriginalTheme.colors.text.primary,
                LocalTextStyle provides MaterialTheme.typography.subtitle2,
            ) {
                content()
            }
            CompositionLocalProvider(
                LocalContentColor provides MegaOriginalTheme.colors.text.primary,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(1.dp)
                        .background(
                            color = MegaOriginalTheme.colors.background.pageBackground,
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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