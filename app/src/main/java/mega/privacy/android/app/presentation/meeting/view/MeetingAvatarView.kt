package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012

/**
 * Avatar view for a Meeting user that includes default Placeholder
 *
 * @param modifier
 * @param avatarUri
 * @param avatarPlaceholder
 * @param avatarColor
 * @param avatarTimestamp
 */
@Composable
fun MeetingAvatarView(
    modifier: Modifier = Modifier,
    avatarUri: String?,
    avatarPlaceholder: String?,
    avatarColor: Int?,
    avatarTimestamp: Long? = null,
) {
    val color = avatarColor?.let(::Color) ?: MaterialTheme.colors.grey_012_white_012
    if (avatarUri.isNullOrBlank()) {
        AvatarPlaceholderView(
            char = avatarPlaceholder?.let(::getAvatarFirstLetter) ?: "U",
            backgroundColor = color,
            modifier = modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    } else {
        AvatarView(
            avatarUri = avatarUri,
            placeholderColor = color,
            avatarTimestamp = avatarTimestamp,
            modifier = modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

@Composable
private fun AvatarPlaceholderView(
    modifier: Modifier = Modifier,
    char: String,
    backgroundColor: Color,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val currentHeight = placeable.height
                var heightCircle = currentHeight
                if (placeable.width > heightCircle)
                    heightCircle = placeable.width

                layout(heightCircle, heightCircle) {
                    placeable.placeRelative(0, (heightCircle - currentHeight) / 2)
                }
            }
    ) {
        Text(
            text = char,
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.subtitle1,
            fontSize = (maxWidth.value / 1.8).sp,
        )
    }
}

@Composable
private fun AvatarView(
    modifier: Modifier = Modifier,
    avatarUri: String,
    placeholderColor: Color,
    avatarTimestamp: Long? = null,
) {
    val visiblePlaceholder = remember { mutableStateOf(true) }
    AsyncImage(
        modifier = modifier.placeholder(
            visible = visiblePlaceholder.value,
            color = placeholderColor,
            shape = CircleShape,
            highlight = PlaceholderHighlight.shimmer(placeholderColor),
        ),
        model = ImageRequest.Builder(LocalContext.current)
            .setParameter("timestamp", avatarTimestamp)
            .data(avatarUri)
            .build(),
        contentDescription = "User avatar",
        onSuccess = { visiblePlaceholder.value = false }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewMeetingAvatarView")
@Composable
private fun PreviewMeetingAvatarView() {
    MeetingAvatarView(
        avatarUri = null,
        avatarPlaceholder = "D",
        avatarColor = "#00FFFF".toColorInt(),
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, Color.White, CircleShape)
    )
}
