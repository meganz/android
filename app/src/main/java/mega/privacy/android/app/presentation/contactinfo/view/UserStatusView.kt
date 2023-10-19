package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.app.presentation.extensions.text
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.domain.entity.contacts.UserChatStatus

@Composable
internal fun UserStatusView(
    title: String,
    userChatStatus: UserChatStatus,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val primaryTextColor = lerp(white_alpha_087, MaterialTheme.colors.textColorPrimary, progress)
    val secondaryTextColor =
        lerp(white_alpha_087, MaterialTheme.colors.textColorSecondary, progress)
    val iconAlpha = (1 - progress * 2).coerceAtLeast(0f)
    val maxLines = if (progress < 0.5f) 2 else 1
    Column(
        modifier = modifier
            .wrapContentHeight()
    ) {
        TextWithTrailingIcon(
            text = title,
            iconAlpha = iconAlpha,
            imageResource = userChatStatus.iconRes(MaterialTheme.colors.isLight),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textStyle = MaterialTheme.typography.h6.copy(
                color = primaryTextColor,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = stringResource(id = userChatStatus.text),
            modifier = modifier,
            style = MaterialTheme.typography.body2.copy(
                color = secondaryTextColor,
            ),
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewUserStatusLight() {
    AndroidTheme(isDark = false) {
        Surface {
            UserStatusView(
                modifier = Modifier,
                title = "Nick name :P two lien test strrijaskjkhklsdjhglj",
                userChatStatus = UserChatStatus.Online,
                progress = 1f
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewUserStatusDark() {
    AndroidTheme(isDark = true) {
        Surface {
            UserStatusView(
                modifier = Modifier,
                title = "Nick name",
                userChatStatus = UserChatStatus.Online,
                progress = 0.05f
            )
        }
    }
}