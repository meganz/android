package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case to check if Node file is valid
 */
class IsValidNodeFileUseCase @Inject constructor(private val fileSystemRepository: FileSystemRepository) {

    /**
     * check if Node file is valid
     *
     * @param node
     * @param file
     *
     * @return Boolean
     */
    suspend operator fun invoke(
        node: TypedFileNode,
        file: File,
    ): Boolean =
        file.canRead() && file.length() == node.size && node.fingerprint == fileSystemRepository.getFingerprint(
            file.absolutePath
        )
}
