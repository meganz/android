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

/**
 * Chat avatar
 *
 * @param handle User handle
 * @param modifier Modifier
 * @param lastUpdatedCache Last update time of the avatar

 */
@Composable
fun ChatAvatar(
    handle: Long,
    modifier: Modifier = Modifier,
    lastUpdatedCache: Long = 0L,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(
                ContactAvatar(id = UserId(handle))
            )
            .transformations(CircleCropTransformation())
            .memoryCacheKey("${handle}_$lastUpdatedCache}")
            .build(),
        contentDescription = "Avatar",
        contentScale = ContentScale.Inside,
        modifier = modifier.size(24.dp)
    )
}