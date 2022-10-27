package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default implementation of [CompleteFastLogin].
 *
 * @property loginRepository [LoginRepository].
 */
class DefaultCompleteFastLogin @Inject constructor(
    private val loginRepository: LoginRepository,
) : CompleteFastLogin {

    override suspend fun invoke(session: String) {
        kotlin.runCatching { loginRepository.initMegaChat(session) }
            .onFailure { exception -> throw exception }
            .onSuccess {
                kotlin.runCatching { loginRepository.fastLogin(session) }
                    .onFailure { exception -> throw exception }
                    .onSuccess {
                        kotlin.runCatching { loginRepository.fetchNodes() }
                            .onFailure { exception -> throw exception }
                    }
            }
    }
}