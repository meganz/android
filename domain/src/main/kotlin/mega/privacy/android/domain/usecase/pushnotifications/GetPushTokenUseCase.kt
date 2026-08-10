package mega.privacy.android.domain.usecase.pushnotifications

import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Gets push token use case.
 */
class GetPushTokenUseCase @Inject constructor(
    private val pushesRepository: PushesRepository,
) {

    /**
     * Invoke
     *
     * @return Push token.
     */
    operator fun invoke(): String = pushesRepository.getPushToken()
}
