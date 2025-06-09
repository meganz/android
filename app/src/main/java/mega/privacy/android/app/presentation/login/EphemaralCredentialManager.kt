package mega.privacy.android.app.presentation.login

import mega.privacy.android.domain.entity.login.EphemeralCredentials
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ephemeral Credential Manager to store and manage ephemeral credentials in memory.
 */
@Singleton
class EphemeralCredentialManager @Inject constructor() {
    private val ephemeralCredential: AtomicReference<EphemeralCredentials?> = AtomicReference()

    /**
     * Gets the current ephemeral credentials.
     *
     * @return the current [EphemeralCredentials] or null if not set.
     */
    fun getEphemeralCredential(): EphemeralCredentials? {
        return ephemeralCredential.get()
    }

    /**
     * Sets the ephemeral credentials.
     *
     * @param ephemeralCredentials the [EphemeralCredentials] to set.
     */
    fun setEphemeralCredential(ephemeralCredentials: EphemeralCredentials?) {
        ephemeralCredential.set(ephemeralCredentials)
    }
}