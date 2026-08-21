package mega.privacy.android.feature.sharelink.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory, session-only cache of the full public link — decryption key included — keyed by node
 * handle or album id. Never persisted to disk.
 *
 * The Link settings screen reads the node behind the link to find it, which works for a file or a
 * folder but not for an album: an album is not a node. Without the link, saving an album's settings
 * could put nothing back on the clipboard, so a user who had just separated the link and key was
 * left holding the earlier link — key and all — and would paste that.
 *
 * The Share link screen resolves the link for either subject, so it publishes it here for the
 * settings screen to pick up.
 */
@Singleton
class ShareLinkPublicLinkCache @Inject constructor() {

    private val links = mutableMapOf<Long, String>()

    /** The full public link cached for [key], or null when none has been resolved yet. */
    fun get(key: Long): String? = synchronized(links) { links[key] }

    /** Publishes the full public link resolved for [key]. Blank links are ignored. */
    fun set(key: Long, link: String) {
        if (link.isBlank()) return
        synchronized(links) { links[key] = link }
    }
}
