package mega.privacy.android.shared.contact.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.contact.model.AvatarData

/**
 * Renders a single circular avatar (photo or coloured initials) with an
 * optional verified-contact badge overlay.
 *
 * @param avatar Source for the avatar (image file or initials + colour).
 * @param displayName Used as the image content description.
 * @param isVerified When true, overlays the verified-contact badge.
 * @param modifier
 */
@Composable
fun ContactAvatar(
    avatar: AvatarData,
    displayName: String,
    isVerified: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.testTag(CONTACT_ITEM_VIEW_AVATAR)) {
        when (avatar) {
            is AvatarData.Image -> MediumProfilePicture(
                imageFile = avatar.file,
                name = null,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
            )

            is AvatarData.Initials -> MediumProfilePicture(
                imageFile = null,
                name = avatar.initials,
                contentDescription = displayName,
                avatarColor = avatar.avatarColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isVerified) {
            Image(
                painter = painterResource(R.drawable.ic_contact_verified),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(14.dp)
                    .testTag(CONTACT_ITEM_VIEW_VERIFIED_BADGE),
            )
        }
    }
}

internal const val CONTACT_ITEM_VIEW_AVATAR = "contact_item_view:avatar"

@CombinedThemePreviews
@Composable
private fun ContactAvatarPreview() {
    AndroidThemeForPreviews {
        ContactAvatar(
            avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
            displayName = "",
            isVerified = true,
            modifier = Modifier.size(32.dp)
        )
    }
}