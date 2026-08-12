package mega.privacy.android.feature.sync.domain.entity

/**
 * The condition that is currently preventing syncs from running.
 */
enum class SyncPauseReason {
    /**
     * Battery is below the low battery threshold and the device is not charging
     */
    LowBattery,

    /**
     * The device is in battery saver mode, the user opted into pausing for it,
     * and the device is not charging
     */
    BatterySaver,

    /**
     * Syncing is restricted to Wi-Fi and the device is not on Wi-Fi
     */
    NoWifi,

    /**
     * Syncing is restricted to while charging and the device is not charging
     */
    NotCharging,
}
