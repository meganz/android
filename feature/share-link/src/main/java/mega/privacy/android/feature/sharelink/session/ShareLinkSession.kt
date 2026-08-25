package mega.privacy.android.feature.sharelink.session

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Everything the Share link and Link settings screens hold about a link only for as long as the
 * user is looking at it: the password, the "separate link and key" choice and the resolved link
 * itself. None of it is a node property, so none of it can be read back from the SDK.
 *
 * The session exists for one visit to the Share link flow. It is created by
 * [mega.privacy.android.feature.sharelink.presentation.ShareLinkSessionViewModel], which lives in
 * the navigation scope the Share link entry owns, and both screens are handed the same instance for
 * as long as either is on the back stack.
 *
 * That lifetime is the point. This state used to be held process-wide and keyed by node handle, and
 * a handle outlives its link: removing a link and creating a new one for the same node reuses the
 * handle, so the old link's password and key choice attached themselves to the new link, which was
 * then shown as protected and shared in a password-encrypted form of a link that no longer existed.
 * Leaving the flow ends the session, so the next visit starts from what the SDK actually reports.
 *
 * **One session serves one subject.** The flow is opened for a single subject — one album, or one
 * node selection of which only the first is editable — and it is a leaf: nothing reachable from it
 * opens another Share link screen. So there is one link's worth of state here, held directly rather
 * than in a map keyed by handle. If the flow ever gains a second entry point on top of itself, this
 * is the assumption that breaks.
 *
 * The fields are mutable and public because both screens write to them; there is no reader-only
 * side to protect. Nothing here is persisted to disk.
 */
class ShareLinkSession {

    /**
     * The password protecting the link, or null when it is not protected. Observable because the
     * Share link screen reflects a change made on the Link settings screen without being reopened.
     */
    val password = MutableStateFlow<LinkPassword?>(null)

    /** Whether the link is shared without its decryption key. Observable for the same reason. */
    val isKeySeparate = MutableStateFlow(false)

    /**
     * The full public link, decryption key included, as the Share link screen resolved it.
     *
     * Link settings reads the node behind the link to find it, which works for a file or a folder
     * but not for an album: an album is not a node. Without the link, saving an album's settings
     * could put nothing back on the clipboard, leaving a user who had just separated the link and
     * key holding the earlier link — key and all — and pasting that.
     *
     * Read once when Link settings opens rather than observed, so a plain value.
     */
    var publicLink: String? = null
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
