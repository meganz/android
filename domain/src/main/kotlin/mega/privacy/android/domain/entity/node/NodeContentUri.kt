package mega.privacy.android.domain.entity.node

import java.io.File

/**
 * Node content uri
 *
 */
sealed interface NodeContentUri {
    /**
     * Local content uri
     *
     * @property file local file
     */
    data class LocalContentUri(val file: File) : NodeContentUri

    /**
     * Remote content uri
     *
     * @property url remote url
     * @property shouldStopHttpSever should stop http server
     */
    data class RemoteContentUri(val url: String, val shouldStopHttpSever: Boolean) : NodeContentUri
}