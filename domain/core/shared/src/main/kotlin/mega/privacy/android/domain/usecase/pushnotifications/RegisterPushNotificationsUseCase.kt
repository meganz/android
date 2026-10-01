package mega.privacy.android.domain.usecase.pushnotifications

import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * Register push notifications use case.
 */
class RegisterPushNotificationsUseCase @Inject constructor(
    private val pushesRepository: PushesRepository,
) {

    /**
     * Registers push notifications.
     *
     * @param deviceType    Type of device.
     * @param newToken      New push token.
     * @return The push token.
     */
    suspend operator fun invoke(deviceType: Int, newToken: String) =
        pushesRepository.registerPushNotifications(deviceType, newToken)
}