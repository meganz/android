package mega.privacy.android.domain.entity.node

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.serialisation.FileSerializer
import java.io.File

/**
 * Node content uri
 *
 */
@Polymorphic
sealed interface NodeContentUri {
    /**
     * Local content uri
     *
     * @property file local file
     */
    @Serializable
    data class LocalContentUri(
        @Serializable(with = FileSerializer::class)
        val file: File,
    ) : NodeContentUri

    /**
     * Remote content uri
     *
     * @property url remote url
     * @property shouldStopHttpSever should stop http server
     */
    @Serializable
    data class RemoteContentUri(val url: String, val shouldStopHttpSever: Boolean) : NodeContentUri
}