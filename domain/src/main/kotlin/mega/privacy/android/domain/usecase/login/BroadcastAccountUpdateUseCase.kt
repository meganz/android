package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Use case for notifying an account update.
 */
class BroadcastAccountUpdateUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = loginRepository.broadcastAccountUpdate()
}