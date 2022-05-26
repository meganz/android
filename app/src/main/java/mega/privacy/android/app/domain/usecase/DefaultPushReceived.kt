package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Default [PushReceived] implementation.
 *
 * @property pushesRepository   [PushesRepository]
 */
class DefaultPushReceived @Inject constructor(
    private val pushesRepository: PushesRepository
) : PushReceived {

    override suspend fun invoke(beep: Boolean) {
        pushesRepository.pushReceived(beep)
    }
}