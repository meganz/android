package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Rounded bubble that holds the content of a chat message.
 *
 * Own messages use the primary brand colour and other messages use a surface colour. The bubble
 * provides a matching content colour and text style to [content]. The optional [subContent] shows
 * below [content] on a page-background panel, for example a link preview.
 *
 * @param isMine True when the message was sent by the current user.
 * @param modifier [Modifier]
 * @param subContent Optional content shown below [content] inside the bubble.
 * @param content Main content of the bubble.
 */
@Composable
fun ChatBubble(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    subContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(modifier)
            .background(
                color = if (isMine) DSTokens.colors.button.primary else DSTokens.colors.background.surface2,
            )
            .testTag(CHAT_BUBBLE_TAG),
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            CompositionLocalProvider(
                LocalContentColor provides if (isMine) DSTokens.colors.text.inverse else DSTokens.colors.text.primary,
                LocalTextStyle provides AppTheme.typography.titleSmall,
            ) {
                content()
            }
            subContent?.let {
                CompositionLocalProvider(
                    LocalContentColor provides DSTokens.colors.text.primary,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(1.dp)
                            .background(
                                color = DSTokens.colors.background.pageBackground,
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            )
                            .testTag(CHAT_BUBBLE_SUB_CONTENT_TAG),
                    ) {
                        it()
                    }
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChatBubblePreview(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    AndroidThemeForPreviews {
        ChatBubble(
            isMine = isMine,
            subContent = {
                MegaText(
                    text = "Sub content",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        ) {
            MegaText(
                text = "Hello world!",
                textColor = if (isMine) TextColor.Inverse else TextColor.Primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Test tag for the chat bubble.
 */
const val CHAT_BUBBLE_TAG = "chat_bubble:bubble"

/**
 * Test tag for the sub content panel of the chat bubble.
 */
const val CHAT_BUBBLE_SUB_CONTENT_TAG = "chat_bubble:sub_content"
