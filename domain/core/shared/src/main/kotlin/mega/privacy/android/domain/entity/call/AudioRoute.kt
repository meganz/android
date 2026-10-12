package mega.privacy.android.domain.entity.call

/**
 * Describes where call audio plays now and where it can play.
 *
 * @property current The device that plays the call audio now. [AudioDevice.None] when no call
 * audio session is active.
 * @property available The devices that the call audio can be routed to now. Never contains
 * [AudioDevice.None], and is empty when no call audio session is active.
 */
data class AudioRoute(
    val current: AudioDevice,
    val available: Set<AudioDevice>,
)
