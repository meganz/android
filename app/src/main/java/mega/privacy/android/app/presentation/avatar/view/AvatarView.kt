package mega.privacy.android.app.presentation.avatar.view

import android.content.res.Configuration
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.avatar.model.EmojiAvatarContent
import mega.privacy.android.app.presentation.avatar.model.PhotoAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.dark_grey
import mega.privacy.android.shared.original.core.ui.theme.white

/**
 * Avatar Composable
 */
@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    content: AvatarContent,
) {
    when (content) {
        is TextAvatarContent -> {
            TextAvatar(
                modifier = modifier,
                avatarBgColor = content.backgroundColor,
                firstLetter = content.avatarText,
                showBorder = content.showBorder,
                textSize = content.textSize
            )
        }

        is EmojiAvatarContent -> {
            EmojiAvatar(
                modifier = modifier,
                avatarBgColor = content.backgroundColor,
                resId = content.emojiContent,
                showBorder = content.showBorder
            )
        }

        is PhotoAvatarContent -> {
            PhotoAvatar(
                modifier = modifier,
                content = content,
            )
        }
    }
}

/**
 * A default avatar with user's first letter in the center.
 *
 * @param modifier
 * @param avatarBgColor background color of the avatar
 * @param firstLetter first letter of user name
 */
@Composable
fun TextAvatar(
    modifier: Modifier,
    @ColorInt avatarBgColor: Int,
    firstLetter: String,
    showBorder: Boolean,
    textSize: TextUnit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AvatarBackground(avatarBgColor = avatarBgColor, showBorder = showBorder)

        Text(
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .testTag("TextAvatar"),
            text = firstLetter,
            color = Color.White,
            fontSize = textSize,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif
        )
    }
}

/**
 * A default avatar with user's emoji in the center
 *
 * @param modifier
 * @param avatarBgColor background color of the avatar
 * @param resId first letter of user name
 */
@Composable
fun EmojiAvatar(
    modifier: Modifier,
    avatarBgColor: Int,
    showBorder: Boolean,
    @DrawableRes resId: Int,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AvatarBackground(avatarBgColor = avatarBgColor, showBorder = showBorder)

        Image(
            painter = painterResource(id = resId),
            contentDescription = "avatar emoji",
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
                .testTag("EmojiAvatar")
        )
    }
}

@Composable
fun PhotoAvatar(
    modifier: Modifier = Modifier,
    content: PhotoAvatarContent,
) {
    key(content.size, content.path) {
        AsyncImage(
            modifier = modifier
                .clip(CircleShape)
                .testTag("PhotoAvatar")
                .run {
                    if (content.showBorder) {
                        border(
                            width = 3.dp,
                            color = borderColor(),
                            shape = CircleShape
                        )
                    } else this
                },
            model = ImageRequest.Builder(LocalContext.current)
                .data(content.path)
                .addLastModifiedToFileCacheKey(true)
                .build(),
            contentDescription = "Photo Avatar"
        )
    }
}

@Composable
private fun AvatarBackground(
    modifier: Modifier = Modifier,
    avatarBgColor: Int,
    showBorder: Boolean,
) {
    Image(
        painter = ColorPainter(color = Color(avatarBgColor)),
        contentDescription = null,
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .testTag("AvatarBackground")
            .run {
                if (showBorder) {
                    border(
                        width = 3.dp,
                        color = borderColor(),
                        shape = CircleShape
                    )
                } else this
            }
    )
}

@Composable
private fun borderColor() = white.takeIf { MaterialTheme.colors.isLight } ?: dark_grey

/**
 * PreviewEmojiAvatar
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "PreviewEmojiAvatar")
@Composable
fun PreviewEmojiAvatar() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        EmojiAvatar(
            modifier = Modifier.size(30.dp),
            avatarBgColor = Color.Blue.toArgb(),
            resId = R.drawable.emoji_twitter_1f604,
            showBorder = true
        )
    }
}


/**
 * Preview TextAvatar
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "PreviewTextAvatar")
@Composable
fun PreviewTextAvatar() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        TextAvatar(
            modifier = Modifier.size(66.dp),
            avatarBgColor = Color.Magenta.toArgb(),
            firstLetter = "R",
            showBorder = true,
            textSize = 38.sp,
        )
    }
}
