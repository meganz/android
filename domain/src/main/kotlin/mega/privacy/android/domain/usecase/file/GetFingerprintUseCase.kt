package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the fingerprint by file path
 */
class GetFingerprintUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * Get the fingerprint by file path
     *
     * @param filePath
     * @return fingerprint string or null
     */
    suspend operator fun invoke(filePath: String) = nodeRepository.getFingerprint(filePath)
}
