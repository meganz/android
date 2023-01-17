package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Can text file be opened internally
 */
fun interface CanTextFileBeOpenedInternally {
    /**
     * Invoke
     *
     * @param node
     * @return true if file can be opened internally
     */
    suspend operator fun invoke(node: TypedFileNode): Boolean
}