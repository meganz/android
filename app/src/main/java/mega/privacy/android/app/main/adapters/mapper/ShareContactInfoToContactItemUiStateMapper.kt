package mega.privacy.android.app.main.adapters.mapper

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import nz.mega.sdk.MegaChatApi
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a [ShareContactInfo] row (Mega contact or phone contact) plus the
 * Android-resolved auxiliary data (avatar file, avatar color, online status,
 * verification flag) into the presentational [ContactItemUiState] consumed by
 * `ContactItemView` from the `:shared:contact` module.
 *
 * The adapter is responsible for the SDK / Android-side lookups (avatar file
 * on disk, user verification, user online status, palette colour) and passes
 * the results in. This keeps the mapper free of `Context` and the MEGA SDK.
 *
 * @property contactItemStatusMapper Maps [UserChatStatus] to [ContactItemStatus].
 */
class ShareContactInfoToContactItemUiStateMapper @Inject constructor(
    private val contactItemStatusMapper: ContactItemStatusMapper,
) {

    /**
     * Build a [ContactItemUiState] for a single non-header, non-progress row.
     *
     * @param info Source row data.
     * @param mail Resolved e-mail address used as fallback name and avatar key.
     * @param avatarFile On-disk avatar image; `null` falls back to initials.
     * @param avatarColorArgb Android `@ColorInt` ARGB value used as the
     * background colour for the initials avatar.
     * @param chatStatusValue Raw `MegaChatApi.STATUS_*` value; only consulted
     * for Mega contacts.
     * @param isVerified Whether to show the "verified contact" badge; only
     * applies to Mega contacts.
     */
    operator fun invoke(
        info: ShareContactInfo,
        mail: String,
        avatarFile: File?,
        @ColorInt avatarColorArgb: Int,
        chatStatusValue: Int,
        isVerified: Boolean,
    ): ContactItemUiState = when {
        info.isMegaContact -> mapMegaContact(
            megaContact = requireNotNull(info.megaContactAdapter) {
                "Mega contact row must carry a MegaContactAdapter"
            },
            mail = mail,
            avatarFile = avatarFile,
            avatarColor = Color(avatarColorArgb),
            chatStatusValue = chatStatusValue,
            isVerified = isVerified,
        )

        info.isPhoneContact -> mapPhoneContact(
            phoneContact = requireNotNull(info.phoneContactInfo) {
                "Phone contact row must carry a PhoneContactInfo"
            },
            avatarFile = avatarFile,
            avatarColor = Color(avatarColorArgb),
        )

        else -> error("ShareContactInfo must be either a Mega or phone contact")
    }

    private fun mapMegaContact(
        megaContact: MegaContactAdapter,
        mail: String,
        avatarFile: File?,
        avatarColor: Color,
        chatStatusValue: Int,
        isVerified: Boolean,
    ): ContactItemUiState {
        val displayName = megaContact.fullName?.takeIf { it.isNotBlank() } ?: mail
        return ContactItemUiState(
            handle = megaContact.megaUser?.handle ?: 0L,
            displayName = displayName,
            status = contactItemStatusMapper(toUserChatStatus(chatStatusValue)),
            lastSeen = null,
            avatar = buildAvatar(avatarFile, displayName, avatarColor),
            isVerified = isVerified,
        )
    }

    private fun mapPhoneContact(
        phoneContact: PhoneContactInfo,
        avatarFile: File?,
        avatarColor: Color,
    ): ContactItemUiState {
        val displayName = phoneContact.name?.takeIf { it.isNotBlank() }
            ?: phoneContact.email
            ?: ""
        return ContactItemUiState(
            handle = phoneContact.id,
            displayName = displayName,
            status = ContactItemStatus.Unknown,
            lastSeen = null,
            avatar = buildAvatar(avatarFile, displayName, avatarColor),
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

    private fun toUserChatStatus(status: Int): UserChatStatus = when (status) {
        MegaChatApi.STATUS_ONLINE -> UserChatStatus.Online
        MegaChatApi.STATUS_AWAY -> UserChatStatus.Away
        MegaChatApi.STATUS_BUSY -> UserChatStatus.Busy
        MegaChatApi.STATUS_OFFLINE -> UserChatStatus.Offline
        else -> UserChatStatus.Invalid
    }

    private companion object {
        const val UNKNOWN_INITIAL = "U"
    }
}
