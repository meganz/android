package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R

@Composable
fun ChatAvatar(
    handle: Long,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.ic_emoji_smile),
        contentDescription = "Avatar",
        contentScale = ContentScale.Inside,
        modifier = modifier
            .size(24.dp)
    )
}