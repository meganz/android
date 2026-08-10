package mega.privacy.android.domain.entity.preference

/**
 * Stable [FolderPreference] keys for browsing surfaces whose root has no real node handle.
 *
 * A real folder's key is its base64 node handle; these sentinels use a `section:` prefix, which
 * can never collide with a base64 handle or an offline path.
 */
object FolderPreferenceKeys {
    const val INCOMING_SHARES = "section:incoming"
    const val OUTGOING_SHARES = "section:outgoing"
    const val LINKS = "section:links"
}
