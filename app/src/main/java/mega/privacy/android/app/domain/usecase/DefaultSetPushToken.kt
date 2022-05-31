package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Default [SetPushToken] implementation.
 *
 * @property pushesRepository [PushesRepository]
 */
class DefaultSetPushToken @Inject constructor(
private val pushesRepository: PushesRepository
) : SetPushToken{

    override fun invoke(newToken: String) {
        pushesRepository.setPushToken(newToken)
    }
}