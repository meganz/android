package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Serializable

/**
 * Data related to an exported node
 * @param publicLink the url of the public link of the exported node.
 * @param publicLinkCreationTime Creation time for the public link of the node(in seconds since the epoch). 0 if the creation time is not available
 * @param expirationTime Expiry time of the public link, in seconds since the epoch. Null when the link never expires.
 */
@Serializable
data class ExportedData(
    val publicLink: String?,
    val publicLinkCreationTime: Long,
    val expirationTime: Long? = null,
)
