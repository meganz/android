package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SupportRepository
import javax.inject.Inject

/**
 * Get the support email address
 *
 */
class GetSupportEmailUseCase @Inject constructor(
    private val supportRepository: SupportRepository,
) {
    /**
     * Invoke
     *
     * @return Support email address as string
     */
    suspend operator fun invoke() = supportRepository.getSupportEmail()
}