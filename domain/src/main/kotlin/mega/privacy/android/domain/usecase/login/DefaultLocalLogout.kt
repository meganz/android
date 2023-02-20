package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.LocalLogoutApp
import javax.inject.Inject

/**
 * Default implementation of [LocalLogout]
 */
class DefaultLocalLogout @Inject constructor(
    private val loginRepository: LoginRepository,
    private val localLogoutApp: LocalLogoutApp,
) : LocalLogout {

    override suspend fun invoke(clearPsa: ClearPsa) {
        kotlin.runCatching { loginRepository.localLogout() }
            .onSuccess { localLogoutApp(clearPsa) }
    }
}