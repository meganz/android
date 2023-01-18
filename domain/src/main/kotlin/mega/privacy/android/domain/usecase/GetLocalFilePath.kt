package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * The use case for getting local file path
 */
fun interface GetLocalFilePath {

    /**
     * Get the local folder path
     *
     * @param typedFileNode [TypedFileNode]
     * @return local file if it exists
     */
    suspend operator fun invoke(typedFileNode: TypedFileNode?): String?
}