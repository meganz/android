package mega.privacy.android.shared.contact.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.contact.model.AvatarData

/**
 * Renders one or two circular avatars from a list of [AvatarData].
 *
 * Layout follows the legacy `ChatAvatarView(avatars: List<ChatAvatarItem>?)`
 * primitive: a single full-size avatar for one entry, and two 26.dp overlapping
 * avatars for two-or-more entries (last in [Alignment.BottomEnd], first in
 * [Alignment.TopStart], each with a 1.dp white border).
 *
 * @param avatars Avatars to render. Only the first two are used.
 * @param modifier Modifier applied to the container [Box].
 * @param avatarTimestamp Cache-busting hint: when this value changes, the avatar
 * subtree is re-instantiated so any underlying image is reloaded.
 */
@Composable
fun MultiAvatarView(
    avatars: List<AvatarData>,
    modifier: Modifier = Modifier,
    avatarTimestamp: Long? = null,
) {
    Box(
        modifier = modifier,
    ) {
        key(avatarTimestamp) {
            when {
                avatars.isEmpty() -> Unit

                avatars.size == 1 -> ContactAvatar(
                    avatar = avatars.first(),
                    displayName = "",
                    isVerified = false,
                    modifier = Modifier
                        .fillMaxSize()
                )

                else -> {
                    ContactAvatar(
                        avatar = avatars[1],
                        displayName = "",
                        isVerified = false,
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomEnd)
                            .border(1.dp, Color.White, CircleShape),
                    )
                    ContactAvatar(
                        avatar = avatars[0],
                        displayName = "",
                        isVerified = false,
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.TopStart)
                            .border(1.dp, Color.White, CircleShape),
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MultiAvatarViewSinglePreview() {
    AndroidThemeForPreviews {
        MultiAvatarView(
            avatars = listOf(
                AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
            ),
            modifier = Modifier.size(40.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MultiAvatarViewDoublePreview() {
    AndroidThemeForPreviews {
        MultiAvatarView(
            avatars = listOf(
                AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
            ),
            modifier = Modifier.size(40.dp),
        )
    }
}
