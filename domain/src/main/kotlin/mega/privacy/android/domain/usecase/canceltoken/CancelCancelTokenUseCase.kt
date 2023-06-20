package mega.privacy.android.domain.usecase.canceltoken

import mega.privacy.android.domain.repository.CancelTokenRepository
import javax.inject.Inject

/**
 * Cancel and invalidates the current cancel token, if exists
 */
class CancelCancelTokenUseCase @Inject constructor(
    private val cancelTokenRepository: CancelTokenRepository,
) {

    /**
     * Cancel and invalidates the current cancel token, if exists
     */
    suspend operator fun invoke() = cancelTokenRepository.cancelCurrentToken()
}