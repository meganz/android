package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Broadcast Finish Activity
 *
 */
class BroadcastFinishActivityUseCase @Inject constructor(private val loginRepository: LoginRepository) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = loginRepository.broadcastFinishActivity()
}