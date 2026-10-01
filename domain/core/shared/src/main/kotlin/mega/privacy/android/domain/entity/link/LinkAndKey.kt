package mega.privacy.android.domain.entity.link

/**
 * A public link split into the link without its decryption key, and the key itself.
 *
 * @property linkWithoutKey The link with the decryption key removed, or null if it could not be split.
 * @property key The decryption key, or null if it could not be split.
 */
data class LinkAndKey(
    val linkWithoutKey: String?,
    val key: String?,
)
