package mega.privacy.android.domain.entity.environment

/**
 * An enumeration of different Device Power Connection States
 */
enum class DevicePowerConnectionState {

    /**
     * Power is continually supplied to Device (The Device is now charging)
     */
    Connected,

    /**
     * No power is being supplied to the Device (The Device just stopped charging)
     */
    Disconnected,

    /**
     * An unknown power state
     */
    Unknown,
}