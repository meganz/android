package mega.privacy.android.domain.entity.user

/**
 * User changes
 *
 */
sealed class UserChanges {

    /**
     * User has new or modified authentication information
     *
     */
    data object AuthenticationInformation : UserChanges()

    /**
     * Last interaction timestamp is modified
     *
     */
    data object LastInteractionTimestamp : UserChanges()

    /**
     * User has a new or modified avatar image
     *
     */
    data object Avatar : UserChanges()

    /**
     * User has new or modified firstname
     *
     */
    data object Firstname : UserChanges()

    /**
     * User has new or modified lastname
     *
     */
    data object Lastname : UserChanges()

    /**
     * User has new or modified email
     *
     */
    data object Email : UserChanges()

    /**
     * User has new or modified keyring
     *
     */
    data object Keyring : UserChanges()

    /**
     * User has new or modified country
     *
     */
    data object Country : UserChanges()

    /**
     * User has new or modified birthday
     *
     */
    data object Birthday : UserChanges()

    /**
     * User has new or modified public key for chat
     *
     */
    data object ChatPublicKey : UserChanges()

    /**
     * User has new or modified public key for signing
     *
     */
    data object SigningPublicKey : UserChanges()

    /**
     * User has new or modified signature for RSA public key
     *
     */
    data object RsaPublicKeySignature : UserChanges()

    /**
     * User has new or modified signature for chat public key
     *
     */
    data object ChatPublicKeySignature : UserChanges()

    /**
     * User has new or modified language
     *
     */
    data object Language : UserChanges()

    /**
     * User has new or modified password reminder
     *
     */
    data object PasswordReminder : UserChanges()

    /**
     * User has new or modified disable versions
     *
     */
    data object DisableVersions : UserChanges()

    /**
     * User has new or modified contact link verification
     *
     */
    data object ContactLinkVerification : UserChanges()

    /**
     * User has new or modified rich previews
     *
     */
    data object RichPreviews : UserChanges()

    /**
     * User has new or modified rubbish time
     *
     */
    data object RubbishTime : UserChanges()

    /**
     * User has new or modified storage state
     *
     */
    data object StorageState : UserChanges()

    /**
     * User has new or modified geolocation
     *
     */
    data object Geolocation : UserChanges()

    /**
     * User has new or modified camera uploads folder
     *
     */
    data object CameraUploadsFolder : UserChanges()

    /**
     * User has new or modified my chat files folder
     *
     */
    data object MyChatFilesFolder : UserChanges()

    /**
     * User has new or modified push settings
     *
     */
    data object PushSettings : UserChanges()

    /**
     * User has new or modified alias
     *
     */
    data object Alias : UserChanges()

    /**
     * User has new or modified unshareable key
     *
     */
    data object UnshareableKey : UserChanges()

    /**
     * User has new or modified device names
     *
     */
    data object DeviceNames : UserChanges()

    /**
     * User has new or modified my backups folder
     *
     */
    data object MyBackupsFolder : UserChanges()

    /**
     * User has new or modified cookie settings
     *
     */
    data object CookieSettings : UserChanges()

    /**
     * User has new or modified no callkit
     *
     */
    data object NoCallkit : UserChanges()

    /**
     * User has new or modified visibility
     *  @param userVisibility
     */
    data class Visibility(val userVisibility: UserVisibility) : UserChanges()
}
