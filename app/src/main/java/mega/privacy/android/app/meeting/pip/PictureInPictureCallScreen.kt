package mega.privacy.android.app.meeting.pip

import android.util.Size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.layoutId
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.shared.original.core.ui.controls.video.MegaVideoTextureView

@Composable
internal fun PictureInPictureCallScreen(pictureCallViewModel: PictureInPictureCallViewModel) {
    val state by pictureCallViewModel.uiState.collectAsStateWithLifecycle()
    PictureInPictureCallView(
        isVideoOn = state.isVideoOn,
        peerId = state.peerId,
        videoStream = pictureCallViewModel::getVideoUpdates
    )
}

@Composable
internal fun PictureInPictureCallView(
    isVideoOn: Boolean,
    peerId: Long,
    videoStream: () -> Flow<Pair<Size, ByteArray>>,
) {
    Box(
        modifier = Modifier
            .layoutId("videoPreview")
    ) {
        if (isVideoOn) {
            MegaVideoTextureView(
                videoStream = videoStream(),
                mirrorEffect = true,
                lifecycleState = Lifecycle.State.CREATED,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TEST_TAG_PIP_VIDEO),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)                       // clip to the circle shape
                    .border(2.dp, Color.White, CircleShape)
                    .align(Alignment.Center)
                    .testTag(TEST_TAG_PIP_IMAGE)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            ContactAvatar(id = UserId(peerId))
                        )
                        .transformations(CircleCropTransformation())
                        .memoryCacheKey("${peerId}_0}")
                        .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Inside,
                )
            }
        }
    }
}

internal const val TEST_TAG_PIP_VIDEO = "picture_in_picture:video"
internal const val TEST_TAG_PIP_IMAGE = "picture_in_picture:image"
