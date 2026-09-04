package mega.privacy.android.domain.entity.call

/**
 * Trigger that drives the in-call local-video decision logic.
 *
 * Screen triggers originate from the device lock state ([ScreenOff] / [UserPresent]); proximity
 * triggers originate from the proximity sensor ([ProximityNear] / [ProximityFar]).
 */
sealed interface LocalVideoTrigger {

    /**
     * The screen was turned off (device locked).
     */
    data object ScreenOff : LocalVideoTrigger

    /**
     * The user is present again (device unlocked).
     */
    data object UserPresent : LocalVideoTrigger

    /**
     * The device is close to the ear.
     */
    data object ProximityNear : LocalVideoTrigger

    /**
     * The device is far from the ear.
     */
    data object ProximityFar : LocalVideoTrigger
}
