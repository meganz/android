package mega.privacy.android.feature.documentscanner.domain.repository

/**
 * Persisted user preferences for the new continuous document scanner.
 *
 * Today this carries only the cellular-data consent (a one-time "yes" given
 * via the prepare-screen prompt, after which the user is never asked again).
 * Adding more scanner preferences should land here so the storage stays in
 * one place.
 */
interface ScannerPreferencesRepository {

    /**
     * True if the user has previously consented to downloading the model on a
     * cellular connection. The consent is per-install and never expires.
     */
    suspend fun hasGrantedCellularConsent(): Boolean

    /** Records that the user has agreed to use cellular data for the download. */
    suspend fun grantCellularConsent()
}
