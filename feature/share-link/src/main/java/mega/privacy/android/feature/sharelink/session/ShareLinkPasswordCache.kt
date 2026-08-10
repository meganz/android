package mega.privacy.android.feature.sharelink.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory, session-only cache of link passwords keyed by node handle.
 *
 * MEGA link passwords are client-side and are not a node property, so whether a link is
 * password-protected is only known within the session, after it was set. This cache is the shared
 * source of truth between the Share link screen (which displays the protected state) and the Link
 * settings screen (which sets, changes or removes the password). It is never persisted to disk.
 */
@Singleton
class ShareLinkPasswordCache @Inject constructor() {

    private val entries = MutableStateFlow<Map<Long, LinkPassword>>(emptyMap())

    /** Emits the cached password for [handle], or null when the link is not password-protected. */
    fun monitor(handle: Long): Flow<LinkPassword?> =
        entries.map { it[handle] }.distinctUntilChanged()

    /** Current cached password for [handle], or null. */
    fun get(handle: Long): LinkPassword? = entries.value[handle]

    /** Stores [password] for [handle], or removes the entry when null. */
    fun set(handle: Long, password: LinkPassword?) {
        entries.update { current ->
            if (password == null) current - handle else current + (handle to password)
        }
    }
}

/**
 * A link's password state held in-session.
 *
 * @property password The plaintext password, kept so Link settings can pre-fill it for change.
 * @property linkWithPassword The password-encrypted link to share, or null if encryption failed.
 */
data class LinkPassword(
    val password: String,
    val linkWithPassword: String?,
)
