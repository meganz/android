package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.files.FingerprintRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case to check if Node file is valid
 */
class IsValidNodeFileUseCase @Inject constructor(
    private val fingerprintRepository: FingerprintRepository,
) {

    /**
     * check if Node file is valid
     *
     * @param node
     * @param file
     *
     * @return Boolean
     */
    suspend operator fun invoke(
        node: FileNode,
        file: File,
    ): Boolean = file.canRead()
            && file.length() == node.size
            && node.fingerprint == fingerprintRepository.getFingerprint(file.absolutePath)
}
