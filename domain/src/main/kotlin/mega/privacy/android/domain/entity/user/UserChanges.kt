package mega.privacy.android.domain.entity.user

/**
 * User changes
 *
 */
enum class UserChanges {
    /**
     * User has new or modified authentication information
     *
     */
    Authring,

    /**
     * Last interaction timestamp is modified
     *
     */
    Lstint,

    /**
     * User has a new or modified avatar image
     *
     */
    Avatar,

    /**
     * User has new or modified firstname
     *
     */
    Firstname,

    /**
     * User has new or modified lastname
     *
     */
    Lastname,

    /**
     * User has new or modified email
     *
     */
    Email,

    /**
     * User has new or modified keyring
     *
     */
    Keyring,

    /**
     * User has new or modified country
     *
     */
    Country,

    /**
     * User has new or modified birthday
     *
     */
    Birthday,

    /**
     * User has new or modified public key for chat
     *
     */
    PublicKeyCu255,

    /**
     * User has new or modified public key for signing
     *
     */
    PublicKeyEd255,

    /**
     * User has new or modified signature for RSA public key
     *
     */
    SignaturePublicKeyRsa,

    /**
     * User has new or modified signature for chat public key
     *
     */
    SignaturePublicKeyCu255,

    /**
     * User has new or modified language
     *
     */
    Language,

    /**
     * User has new or modified password reminder
     *
     */
    PasswordReminder,

    /**
     * User has new or modified disable versions
     *
     */
    DisableVersions,

    /**
     * User has new or modified contact link verification
     *
     */
    ContactLinkVerification,

    /**
     * User has new or modified rich previews
     *
     */
    RichPreviews,

    /**
     * User has new or modified rubbish time
     *
     */
    RubbishTime,

    /**
     * User has new or modified storage state
     *
     */
    StorageState,

    /**
     * User has new or modified geolocation
     *
     */
    Geolocation,

    /**
     * User has new or modified camera uploads folder
     *
     */
    CameraUploadsFolder,

    /**
     * User has new or modified my chat files folder
     *
     */
    MyChatFilesFolder,

    /**
     * User has new or modified push settings
     *
     */
    PushSettings,

    /**
     * User has new or modified alias
     *
     */
    Alias,

    /**
     * User has new or modified unshareable key
     *
     */
    UnshareableKey,

    /**
     * User has new or modified device names
     *
     */
    DeviceNames,

    /**
     * User has new or modified my backups folder
     *
     */
    MyBackupsFolder,

    /**
     * User has new or modified cookie settings
     *
     */
    CookieSettings,

    /**
     * User has new or modified no callkit
     *
     */
    NoCallkit,
}
