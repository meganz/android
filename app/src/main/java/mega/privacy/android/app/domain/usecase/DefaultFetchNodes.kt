package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default [FetchNodes] implementation.
 *
 * @property loginRepository [LoginRepository]
 */
class DefaultFetchNodes @Inject constructor(
    private val loginRepository: LoginRepository
): FetchNodes {

    override suspend fun invoke() {
        loginRepository.fetchNodes()
    }
}