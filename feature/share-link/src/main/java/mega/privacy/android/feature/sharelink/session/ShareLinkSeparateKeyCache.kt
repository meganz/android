package mega.privacy.android.feature.sharelink.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory, session-only cache of the "separate link and key" preference keyed by node handle.
 *
 * Whether a link is shared without its decryption key is a purely presentational, client-side
 * choice — it is not a node property. This cache is the shared source of truth between the Link
 * settings screen (which sets the preference) and the Share link screen (which reflects it by
 * hiding the key from the link and showing it in a separate card). It is never persisted to disk.
 */
@Singleton
class ShareLinkSeparateKeyCache @Inject constructor() {

    private val handles = MutableStateFlow<Set<Long>>(emptySet())

    /** Emits whether [handle] is currently set to share its link and key separately. */
    fun monitor(handle: Long): Flow<Boolean> =
        handles.map { handle in it }.distinctUntilChanged()

    /** Whether [handle] is currently set to share its link and key separately. */
    fun get(handle: Long): Boolean = handle in handles.value

    /** Sets whether [handle] shares its link and key separately. */
    fun set(handle: Long, separate: Boolean) {
        handles.update { current ->
            if (separate) current + handle else current - handle
        }
    }
}
