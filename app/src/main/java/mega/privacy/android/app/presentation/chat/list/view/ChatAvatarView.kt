package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.chat.ChatAvatarItem

/**
 * Avatar view for a [ChatRoomItem]
 *
 * @param modifier
 * @param avatars
 */
@Composable
fun ChatAvatarView(
    modifier: Modifier = Modifier,
    avatars: List<ChatAvatarItem>?,
) {
    Box(
        modifier.background(Color.Transparent)
    ) {
        when {
            avatars == null -> {
                ChatAvatarView(
                    avatarUri = null,
                    avatarPlaceholder = null,
                    avatarColor = null,
                )
            }

            avatars.size == 1 -> {
                ChatAvatarView(
                    avatarUri = avatars.first().uri,
                    avatarPlaceholder = avatars.first().placeholderText?.takeIf(String::isNotBlank),
                    avatarColor = avatars.first().color,
                )
            }

            else -> {
                ChatAvatarView(
                    avatarUri = avatars.last().uri,
                    avatarPlaceholder = avatars.last().placeholderText?.takeIf(String::isNotBlank),
                    avatarColor = avatars.last().color,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.BottomEnd),
                )
                ChatAvatarView(
                    avatarUri = avatars.first().uri,
                    avatarPlaceholder = avatars.first().placeholderText?.takeIf(String::isNotBlank),
                    avatarColor = avatars.first().color,
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.TopStart),
                )
            }
        }
    }
}

/**
 * Avatar view for a Chat user that includes default Placeholder
 *
 * @param modifier
 * @param avatarUri
 * @param avatarPlaceholder
 * @param avatarColor
 * @param avatarTimestamp
 */
@Composable
fun ChatAvatarView(
    modifier: Modifier = Modifier,
    avatarUri: String?,
    avatarPlaceholder: String?,
    avatarColor: Int?,
    avatarTimestamp: Long? = null,
) {
    val color = avatarColor?.let(::Color) ?: MaterialTheme.colors.grey_alpha_012_white_alpha_012
    when {
        avatarUri.isNullOrBlank() && avatarPlaceholder.isNullOrBlank() && avatarColor == null -> {
            AvatarPlaceholderView(
                char = "",
                backgroundColor = color,
                modifier = modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .placeholder(
                        visible = true,
                        color = Color.LightGray,
                        shape = CircleShape,
                        highlight = PlaceholderHighlight.fade(Color.White),
                    ),
            )
        }

        avatarUri.isNullOrBlank() -> {
            AvatarPlaceholderView(
                char = avatarPlaceholder?.let(::getAvatarFirstLetter) ?: "U",
                backgroundColor = color,
                modifier = modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }

        else -> {
            AvatarImageView(
                avatarUri = avatarUri,
                placeholderColor = color,
                avatarTimestamp = avatarTimestamp,
                modifier = modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
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
private fun AvatarImageView(
    modifier: Modifier = Modifier,
    avatarUri: String,
    placeholderColor: Color,
    avatarTimestamp: Long? = null,
) {
    var showPlaceHolder by remember { mutableStateOf(true) }
    AsyncImage(
        modifier = modifier.placeholder(
            visible = showPlaceHolder,
            color = placeholderColor,
            shape = CircleShape,
            highlight = PlaceholderHighlight.fade(Color.White),
        ),
        model = ImageRequest.Builder(LocalContext.current)
            .setParameter("timestamp", avatarTimestamp)
            .data(avatarUri)
            .build(),
        contentDescription = "User avatar",
        onSuccess = { showPlaceHolder = false }
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewChatAvatarView() {
    ChatAvatarView(
        avatarUri = null,
        avatarPlaceholder = "D",
        avatarColor = "#00FFFF".toColorInt(),
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, Color.White, CircleShape)
    )
}
