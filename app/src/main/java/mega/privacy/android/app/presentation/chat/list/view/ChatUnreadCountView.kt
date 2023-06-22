package mega.privacy.android.app.presentation.chat.list.view

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import kotlin.random.Random

/**
 * Chat unread count icon view
 *
 * @param modifier
 * @param count     Number of unread messages
 */
@Composable
fun ChatUnreadCountView(modifier: Modifier, count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.secondary),
    ) {
        val countText = if (count > 99) "99" else count.toString()
        Text(
            text = countText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.surface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewChatUnreadCountView() {
    ChatUnreadCountView(
        Modifier,
        Random.nextInt(110)
    )
}
