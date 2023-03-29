package mega.privacy.android.domain.entity.node

/**
 * Data related to an exported node
 * @param publicLink the url of the public link of the exported node.
 * @param publicLinkCreationTime Creation time for the public link of the node(in seconds since the epoch). 0 if the creation time is not available
 */
data class ExportedData(val publicLink: String?, val publicLinkCreationTime: Long)
