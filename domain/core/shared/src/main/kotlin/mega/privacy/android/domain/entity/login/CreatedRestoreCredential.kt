package mega.privacy.android.domain.entity.login

/**
 * Result of storing a restore credential on the device.
 *
 * @property responseJson Registration response to send to the server. It is opaque to the client.
 * @property isBackedUpToCloud True if the credential is included in the device's cloud backup,
 * false if it is kept on this device only.
 */
data class CreatedRestoreCredential(
    val responseJson: String,
    val isBackedUpToCloud: Boolean,
)
