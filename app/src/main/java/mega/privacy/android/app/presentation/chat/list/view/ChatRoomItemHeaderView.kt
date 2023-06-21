package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

/**
 * Chat room item header view
 *
 * @param modifier
 * @param text      Header text
 */
@Composable
fun ChatRoomItemHeaderView(
    modifier: Modifier = Modifier,
    text: String,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .height(36.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewChatRoomItemHeaderView() {
    ChatRoomItemHeaderView(text = "Monday, 23 May")
}
