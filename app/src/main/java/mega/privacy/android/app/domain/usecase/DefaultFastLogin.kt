package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default [FastLogin] implementation.
 *
 * @property loginRepository    Login reporsitory.
 */
class DefaultFastLogin @Inject constructor(
    private val loginRepository: LoginRepository
): FastLogin {

    override suspend fun invoke(session: String) {
        loginRepository.fastLogin(session)
    }
}