package mega.privacy.android.app.contacts.requests.mapper

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a [ContactRequestItem] (incoming or outgoing pending contact request)
 * plus the Android-resolved auxiliary data (avatar file on disk and the
 * palette colour) into the presentational [ContactItemUiState] consumed by
 * `ContactItemView` from the `:shared:contact` module.
 *
 * Contact requests have no online status and are pre-verification, so the
 * mapped state always carries [ContactItemStatus.Unknown] and `isVerified =
 * false`.
 */
class ContactRequestItemToContactItemUiStateMapper @Inject constructor() {

    /**
     * Build a [ContactItemUiState] for a single contact-request row.
     *
     * @param item Source row data.
     * @param avatarFile On-disk avatar image; `null` falls back to initials.
     * @param avatarColorArgb Android `@ColorInt` ARGB value used as the
     * background colour for the initials avatar.
     */
    operator fun invoke(
        item: ContactRequestItem,
        avatarFile: File?,
        @ColorInt avatarColorArgb: Int,
    ): ContactItemUiState {
        val displayName = item.email
        return ContactItemUiState(
            handle = item.handle,
            displayName = displayName,
            status = ContactItemStatus.Unknown,
            lastSeen = null,
            avatar = buildAvatar(
                avatarFile = avatarFile,
                displayName = displayName,
                avatarColor = Color(avatarColorArgb),
            ),
            isVerified = false,
        )
    }

    private fun buildAvatar(
        avatarFile: File?,
        displayName: String,
        avatarColor: Color,
    ): AvatarData = if (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
        AvatarData.Image(file = avatarFile)
    } else {
        AvatarData.Initials(
            initials = firstLetter(displayName),
            avatarColor = avatarColor,
        )
    }

    private fun firstLetter(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return UNKNOWN_INITIAL
        val firstChar = trimmed[0]
        return firstChar.toString().uppercase(Locale.getDefault())
    }

    private companion object {
        const val UNKNOWN_INITIAL = "U"
    }
}
