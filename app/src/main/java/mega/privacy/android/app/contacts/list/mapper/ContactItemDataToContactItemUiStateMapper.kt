package mega.privacy.android.app.contacts.list.mapper

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import nz.mega.sdk.MegaChatApi
import java.io.File
import javax.inject.Inject

/**
 * Maps a [ContactItem.Data] row plus Android-resolved auxiliary data (avatar
 * file, avatar color) into the presentational [ContactItemUiState] consumed by
 * `ContactItemView` from the `:shared:contact` module.
 *
 * The adapter resolves the on-disk avatar file and the ARGB avatar color from
 * the MEGA SDK and passes the results in, keeping the mapper free of `Context`
 * and the SDK.
 *
 * @property contactItemStatusMapper Maps [UserChatStatus] to the
 *  presentational `ContactItemStatus`.
 */
class ContactItemDataToContactItemUiStateMapper @Inject constructor(
    private val contactItemStatusMapper: ContactItemStatusMapper,
) {

    /**
     * Build a [ContactItemUiState] for a single contact data row.
     *
     * @param item Source row data.
     * @param avatarFile On-disk avatar image; `null` (or empty) falls back to
     *  initials.
     * @param avatarColorArgb Android `@ColorInt` ARGB value used as the
     *  background colour for the initials avatar.
     */
    operator fun invoke(
        item: ContactItem.Data,
        avatarFile: File?,
        @ColorInt avatarColorArgb: Int,
    ): ContactItemUiState {
        val displayName = item.getTitle()
        return ContactItemUiState(
            handle = item.handle,
            displayName = displayName,
            status = contactItemStatusMapper(toUserChatStatus(item.status)),
            lastSeen = null,
            avatar = buildAvatar(
                avatarFile = avatarFile,
                initials = item.getFirstCharacter(),
                avatarColor = Color(avatarColorArgb),
            ),
            isVerified = item.isVerified,
        )
    }

    private fun buildAvatar(
        avatarFile: File?,
        initials: String,
        avatarColor: Color,
    ): AvatarData = if (avatarFile != null && avatarFile.exists() && avatarFile.length() > 0) {
        AvatarData.Image(file = avatarFile)
    } else {
        AvatarData.Initials(
            initials = initials,
            avatarColor = avatarColor,
        )
    }

    private fun toUserChatStatus(status: Int?): UserChatStatus = when (status) {
        MegaChatApi.STATUS_ONLINE -> UserChatStatus.Online
        MegaChatApi.STATUS_AWAY -> UserChatStatus.Away
        MegaChatApi.STATUS_BUSY -> UserChatStatus.Busy
        MegaChatApi.STATUS_OFFLINE -> UserChatStatus.Offline
        else -> UserChatStatus.Invalid
    }
}
