package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.UserId

@Composable
fun ChatAvatar(
    handle: Long,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(
                ContactAvatar(id = UserId(handle))
            )
            .transformations(CircleCropTransformation())
            .build(),
        contentDescription = "Avatar",
        contentScale = ContentScale.Inside,
        modifier = modifier.size(24.dp)
    )
}