package mega.privacy.android.domain.entity.folderlink

/**
 * Class representing result of fetchNodes
 *
 * @property nodeHandle
 * @property flag
 */
data class FetchNodeRequestResult(
    val nodeHandle: Long,
    val flag: Boolean
)