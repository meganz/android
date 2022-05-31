package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default [InitMegaChat] implementation.
 *
 * @property loginRepository [LoginRepository]
 */
class DefaultInitMegaChat @Inject constructor(
    private val loginRepository: LoginRepository
) : InitMegaChat {

    override suspend fun invoke(session: String) {
        loginRepository.initMegaChat(session)
    }
}