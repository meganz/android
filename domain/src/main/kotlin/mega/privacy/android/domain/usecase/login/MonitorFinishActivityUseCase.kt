package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Monitor Finish Activity
 *
 */
class MonitorFinishActivityUseCase @Inject constructor(private val loginRepository: LoginRepository) {
    /**
     * Invoke
     *
     * @return Flow
     */
    operator fun invoke() = loginRepository.monitorFinishActivity()
}