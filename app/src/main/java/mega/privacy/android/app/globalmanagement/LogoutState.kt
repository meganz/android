package mega.privacy.android.app.globalmanagement

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped holder for the logout-in-progress flag.
 *
 * Kept as an injectable [Singleton] so its lifetime matches the process, replacing the former
 * mutable static on `MegaApplication`.
 */
@Singleton
class LogoutState @Inject constructor() {
    /**
     * Whether a logout is currently in progress.
     */
    var isLoggingOut = false
}
