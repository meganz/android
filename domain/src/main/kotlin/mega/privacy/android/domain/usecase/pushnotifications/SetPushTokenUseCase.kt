package mega.privacy.android.domain.usecase.pushnotifications

import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Set push token use case.
 */
class SetPushTokenUseCase @Inject constructor(
    private val pushesRepository: PushesRepository,
) {

    /**
     * Invoke
     *
     * @param newToken The push token.
     */
    operator fun invoke(newToken: String) = pushesRepository.setPushToken(newToken)
}