package mega.privacy.android.data.repository

import nz.mega.sdk.MegaPushNotificationSettings

/**
 * Temporary repository to act as a bridge between [PushSettingsManagement] and [DefaultNotificationsRepository]
 * to  [MegaPushNotificationSettings] belonging to the data layer to the presentation layer
 *
 * This repository does not comply with architecture convention and is meant to be removed once [PushSettingsManagement]
 * is removed
 *
 */
interface LegacyNotificationRepository {

    /**
     * Get the [MegaPushNotificationSettings]
     */
    val pushNotificationSettings: MegaPushNotificationSettings

}
