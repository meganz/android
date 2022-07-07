package mega.privacy.android.domain.usecase

/**
 * Register push notifications use case.
 */
fun interface RegisterPushNotifications {

    /**
     * Registers push notifications.
     *
     * @param deviceType    Type of device.
     * @param newToken      New push token.
     * @return The push token.
     */
    suspend operator fun invoke(deviceType: Int, newToken: String): String
}