package mega.privacy.android.data.preferences.base

import com.google.crypto.tink.StreamingAead

/**
 * Reported when a preference file could not be parsed and was replaced with an empty one,
 * discarding every preference it held.
 */
internal class PreferenceDataStoreCorruptionException(fileName: String, cause: Throwable) :
    RuntimeException("Preference datastore $fileName was corrupt and has been reset", cause)

/**
 * Reported when an encrypted preference store could not be created and an unencrypted store is
 * used in its place, leaving the preferences in plaintext on disk.
 */
internal class EncryptedPreferenceFallbackException(fileName: String, cause: Throwable) :
    RuntimeException("Falling back to an unencrypted datastore for $fileName", cause)

/**
 * Thrown when the [StreamingAead] required to encrypt a preference store is unavailable.
 */
internal class StreamingAeadUnavailableException(fileName: String) :
    IllegalStateException("No StreamingAead available to encrypt $fileName")
