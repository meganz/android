package mega.privacy.android.domain.entity.environment

/**
 * The lock state of the device screen.
 */
enum class ScreenLockState {

    /**
     * The screen was turned off (device locked).
     */
    Locked,

    /**
     * The user is present again (device unlocked).
     */
    Unlocked,
}
