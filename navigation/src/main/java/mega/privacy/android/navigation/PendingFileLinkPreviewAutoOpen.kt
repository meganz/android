package mega.privacy.android.navigation

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot, process-wide signal that the next file link to load should auto-open its preview.
 * Resumes a logged-out "Save to MEGA" flow: the player arms it before handing off to login, and
 * the file link screen consumes it once the link reloads after authentication (the pending deep
 * link guarantees that reopened link is the one the user came from).
 *
 * Lives alongside [setPendingDeepLink] as a transient post-authentication navigation signal.
 */
@Singleton
class PendingFileLinkPreviewAutoOpen @Inject constructor() {
    private val armed = AtomicBoolean(false)

    fun arm() {
        armed.set(true)
    }

    fun consume(): Boolean = armed.getAndSet(false)
}
