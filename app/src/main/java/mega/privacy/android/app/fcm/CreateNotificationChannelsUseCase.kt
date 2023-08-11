package mega.privacy.android.app.fcm

import javax.inject.Inject

/**
 * Use case to create the required app Notification Channels
 *
 */
internal class CreateNotificationChannelsUseCase @Inject constructor(
    private val createChatNotificationChannelsUseCase: CreateChatNotificationChannelsUseCase,
    private val createTransferNotificationChannelsUseCase: CreateTransferNotificationChannelsUseCase,
) {

    /**
     * Create the required app notification Channels
     */
    operator fun invoke() {
        createChatNotificationChannelsUseCase()
        createTransferNotificationChannelsUseCase()
    }
}