package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Use case for monitoring an account update.
 */
class MonitorAccountUpdateUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = loginRepository.monitorAccountUpdate()
}