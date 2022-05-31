package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Default [RegisterPushNotifications] implementation.
 *
 * @property pushesRepository [PushesRepository]
 */
class DefaultRegisterPushNotifications @Inject constructor(
    private val pushesRepository: PushesRepository
) : RegisterPushNotifications {

    override suspend fun invoke(deviceType: Int, newToken: String): String =
        pushesRepository.registerPushNotifications(deviceType, newToken)

}