package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import nz.mega.sdk.MegaUser

/**
 * Mapper to convert list of updated users from sdk into a [UserUpdate] entity
 */
typealias UserUpdateMapper = (@JvmSuppressWildcards List<@JvmSuppressWildcards MegaUser>) -> UserUpdate


/**
 * Maps from mega user list to UserUpdate
 *
 * @param userList
 */
internal fun mapMegaUserListToUserUpdate(userList: List<MegaUser>) = UserUpdate(
    userList.groupBy { user -> UserId(user.handle) }
        .mapValues { (_, users) ->
            users.map { i -> fromMegaUserChangeFlags(i.changes) }.flatten()
        }
)

private fun fromMegaUserChangeFlags(changeFlags: Int) =
    if (changeFlags == 0) emptyList() // MegaUser visibility change
    else userChangesMap.filter { it.key and changeFlags != 0 }.values.toList()

private val userChangesMap = mapOf(
    MegaUser.CHANGE_TYPE_AUTHRING to UserChanges.Authring,
    MegaUser.CHANGE_TYPE_LSTINT to UserChanges.Lstint,
    MegaUser.CHANGE_TYPE_AVATAR to UserChanges.Avatar,
    MegaUser.CHANGE_TYPE_FIRSTNAME to UserChanges.Firstname,
    MegaUser.CHANGE_TYPE_LASTNAME to UserChanges.Lastname,
    MegaUser.CHANGE_TYPE_EMAIL to UserChanges.Email,
    MegaUser.CHANGE_TYPE_KEYRING to UserChanges.Keyring,
    MegaUser.CHANGE_TYPE_COUNTRY to UserChanges.Country,
    MegaUser.CHANGE_TYPE_BIRTHDAY to UserChanges.Birthday,
    MegaUser.CHANGE_TYPE_PUBKEY_CU255 to UserChanges.PublicKeyCu255,
    MegaUser.CHANGE_TYPE_PUBKEY_ED255 to UserChanges.PublicKeyEd255,
    MegaUser.CHANGE_TYPE_SIG_PUBKEY_RSA to UserChanges.SignaturePublicKeyRsa,
    MegaUser.CHANGE_TYPE_SIG_PUBKEY_CU255 to UserChanges.SignaturePublicKeyCu255,
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


