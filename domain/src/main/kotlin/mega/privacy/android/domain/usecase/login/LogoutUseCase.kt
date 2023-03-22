package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * LogoutUseCase use case
 */
class LogoutUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = loginRepository.logout()
}
