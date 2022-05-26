package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Default implementation of [GetPushToken]
 *
 * @property pushesRepository
 */
class DefaultGetPushToken @Inject constructor(
    private val pushesRepository: PushesRepository
) : GetPushToken {

    override fun invoke(): String = pushesRepository.getPushToken()
}