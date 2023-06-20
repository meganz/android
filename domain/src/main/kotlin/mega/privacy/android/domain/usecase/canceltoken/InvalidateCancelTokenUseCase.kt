package mega.privacy.android.domain.usecase.canceltoken

import mega.privacy.android.domain.repository.CancelTokenRepository
import javax.inject.Inject

/**
 * Invalidates the current token, it won't be accessible anymore but won't be cancelled
 */
class InvalidateCancelTokenUseCase @Inject constructor(
    private val cancelTokenRepository: CancelTokenRepository,
) {

    /**
     * Invalidates the current token, it won't be accessible anymore but won't be cancelled
     */
    suspend operator fun invoke() = cancelTokenRepository.invalidateCurrentToken()
}