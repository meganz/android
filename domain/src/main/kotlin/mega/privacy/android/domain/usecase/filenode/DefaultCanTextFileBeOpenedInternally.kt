package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.TypedFileNode

class DefaultCanTextFileBeOpenedInternally : CanTextFileBeOpenedInternally {
    override suspend fun invoke(node: TypedFileNode) = node.size <= maxOpenableSizeBytes

    companion object {
        /**
         * Max openable size bytes
         */
        const val maxOpenableSizeBytes: Long = 20_971_520
    }
}