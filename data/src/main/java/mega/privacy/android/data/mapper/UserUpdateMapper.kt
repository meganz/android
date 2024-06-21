package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import javax.inject.Inject

/**
 * Mapper to convert list of updated users from sdk into a [UserUpdate] entity
 */
internal class UserUpdateMapper @Inject constructor() {

    /**
     * Maps from mega user list to UserUpdate
     *
     * @param userList
     */
    operator fun invoke(userList: List<MegaUser>) = UserUpdate(
        changes = userList.groupBy { user -> UserId(user.handle) }
            .mapValues { (_, users) ->
                users.map { i -> fromMegaUserChangeFlags(i.changes, i.visibility) }.flatten()
                    .distinct()
            },
        emailMap = userList.associate { user -> UserId(user.handle) to user.email },
    )
}

private fun fromMegaUserChangeFlags(changeFlags: Long, visibility: Int) =
    userChangesMap.filter { (it.key.toLong() and changeFlags) != 0L }.values.toList() + mapVisibility(
        visibility
    )

private fun mapVisibility(visibility: Int): List<UserChanges> =
    listOf(
        UserChanges.Visibility(
            when (visibility) {
                MegaUser.VISIBILITY_HIDDEN -> UserVisibility.Hidden
                MegaUser.VISIBILITY_VISIBLE -> UserVisibility.Visible
                MegaUser.VISIBILITY_BLOCKED -> UserVisibility.Blocked
                MegaUser.VISIBILITY_UNKNOWN -> UserVisibility.Unknown
                MegaUser.VISIBILITY_INACTIVE -> UserVisibility.Inactive
                else -> UserVisibility.Unknown
            }
        )
    )

private val userChangesMap = mapOf(
    MegaUser.CHANGE_TYPE_AUTHRING to UserChanges.AuthenticationInformation,
    MegaUser.CHANGE_TYPE_LSTINT to UserChanges.LastInteractionTimestamp,
    MegaUser.CHANGE_TYPE_AVATAR to UserChanges.Avatar,
    MegaUser.CHANGE_TYPE_FIRSTNAME to UserChanges.Firstname,
    MegaUser.CHANGE_TYPE_LASTNAME to UserChanges.Lastname,
    MegaUser.CHANGE_TYPE_EMAIL to UserChanges.Email,
    MegaUser.CHANGE_TYPE_KEYRING to UserChanges.Keyring,
    MegaUser.CHANGE_TYPE_COUNTRY to UserChanges.Country,
    MegaUser.CHANGE_TYPE_BIRTHDAY to UserChanges.Birthday,
    MegaUser.CHANGE_TYPE_PUBKEY_CU255 to UserChanges.ChatPublicKey,
    MegaUser.CHANGE_TYPE_PUBKEY_ED255 to UserChanges.SigningPublicKey,
    MegaUser.CHANGE_TYPE_SIG_PUBKEY_RSA to UserChanges.RsaPublicKeySignature,
    MegaUser.CHANGE_TYPE_SIG_PUBKEY_CU255 to UserChanges.ChatPublicKeySignature,
    MegaUser.CHANGE_TYPE_LANGUAGE to UserChanges.Language,
    MegaUser.CHANGE_TYPE_PWD_REMINDER to UserChanges.PasswordReminder,
    MegaUser.CHANGE_TYPE_DISABLE_VERSIONS to UserChanges.DisableVersions,
    MegaUser.CHANGE_TYPE_CONTACT_LINK_VERIFICATION to UserChanges.ContactLinkVerification,
    MegaUser.CHANGE_TYPE_RICH_PREVIEWS to UserChanges.RichPreviews,
    MegaUser.CHANGE_TYPE_RUBBISH_TIME to UserChanges.RubbishTime,
    MegaUser.CHANGE_TYPE_STORAGE_STATE to UserChanges.StorageState,
    MegaUser.CHANGE_TYPE_GEOLOCATION to UserChanges.Geolocation,
    MegaUser.CHANGE_TYPE_CAMERA_UPLOADS_FOLDER to UserChanges.CameraUploadsFolder,
    MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER to UserChanges.MyChatFilesFolder,
    MegaUser.CHANGE_TYPE_PUSH_SETTINGS to UserChanges.PushSettings,
    MegaUser.CHANGE_TYPE_ALIAS to UserChanges.Alias,
    MegaUser.CHANGE_TYPE_UNSHAREABLE_KEY to UserChanges.UnshareableKey,
    MegaUser.CHANGE_TYPE_DEVICE_NAMES to UserChanges.DeviceNames,
    MegaUser.CHANGE_TYPE_MY_BACKUPS_FOLDER to UserChanges.MyBackupsFolder,
    MegaUser.CHANGE_TYPE_COOKIE_SETTINGS to UserChanges.CookieSettings,
    MegaUser.CHANGE_TYPE_NO_CALLKIT to UserChanges.NoCallkit,
)


