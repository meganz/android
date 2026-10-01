package mega.privacy.android.domain.entity.node

import java.io.File

/**
 * Node content uri
 *
 */
sealed interface NodeShareContentUri {
    /**
     * Local content uris
     *
     * @property files
     * @constructor Create empty Local content uri
     */
    data class LocalContentUris(val files: List<File>) : NodeShareContentUri

    /**
     * Remote content uris
     *
     * @property links
     * @constructor Create empty Remote content uri
     */
    data class RemoteContentUris(val links: List<String>) : NodeShareContentUri
}