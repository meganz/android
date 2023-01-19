package mega.privacy.android.domain.usecase.streaming

import mega.privacy.android.domain.entity.node.FileNode

/**
 * Get streaming uri string for node
 */
fun interface GetStreamingUriStringForNode {
    /**
     * Invoke
     *
     * @param node
     * @return streaming uri string
     */
    suspend operator fun invoke(node: FileNode): String?
}