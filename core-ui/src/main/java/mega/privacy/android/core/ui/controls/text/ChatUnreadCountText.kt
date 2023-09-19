package mega.privacy.android.core.ui.controls.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import kotlin.random.Random

/**
 * Chat unread count icon text view
 *
 * @param modifier
 * @param count     Number of unread items
 */
@Composable
fun ChatUnreadCountText(
    modifier: Modifier,
    count: Int,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.secondary),
    ) {
        Text(
            modifier = Modifier.testTag("chat_unread_count:text"),
            text = (count.takeIf { it < MAX_COUNT } ?: MAX_COUNT).toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.surface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val MAX_COUNT = 99

@CombinedThemePreviews
@Composable
private fun PreviewChatUnreadCountView() {
    ChatUnreadCountText(
        Modifier,
        Random.nextInt(110)
    )
}
