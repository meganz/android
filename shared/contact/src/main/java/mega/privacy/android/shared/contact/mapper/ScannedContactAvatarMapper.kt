package mega.privacy.android.shared.contact.mapper

import androidx.compose.ui.graphics.Color
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.shared.contact.model.AvatarData
import javax.inject.Inject

/**
 * Maps a domain [ScannedContactLinkResult] to the presentational [AvatarData] consumed by the
 * scanned-contact dialogs.
 *
 * Produces an [AvatarData.Image] when the scanned contact has an avatar file, otherwise an
 * [AvatarData.Initials] built from the first letter of the contact's name (falling back to the
 * email) and the contact's avatar color. When the result carries no avatar color,
 * [Color.Unspecified] is used so the rendering component falls back to its theme default.
 */
class ScannedContactAvatarMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param result Details of the scanned contact.
     * @return mapped avatar
     */
    operator fun invoke(result: ScannedContactLinkResult): AvatarData =
        result.avatarFile?.let { AvatarData.Image(file = it) }
            ?: AvatarData.Initials(
                initials = result.contactName.ifBlank { result.email }
                    .trim()
                    .take(1)
                    .uppercase(),
                avatarColor = result.avatarColor?.let { Color(it) } ?: Color.Unspecified,
            )
}
