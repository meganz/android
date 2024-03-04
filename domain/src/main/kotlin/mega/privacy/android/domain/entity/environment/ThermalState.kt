package mega.privacy.android.domain.entity.environment

/**
 * Thermal state of the device
 */
enum class ThermalState {

    /**
     * Not under throttling
     */
    ThermalStateNone,

    /**
     * Light throttling where UX is not impacted.
     */
    ThermalStateLight,

    /**
     * Moderate throttling where UX is not largely impacted.
     */
    ThermalStateModerate,

    /**
     * Severe throttling where UX is largely impacted.
     */
    ThermalStateSevere,

    /**
     * Platform has done everything to reduce power.
     */
    ThermalStateCritical,

    /**
     * Key components in platform are shutting down due to thermal condition.
     */
    ThermalStateEmergency,

    /**
     * Need shutdown immediately.
     */
    ThermalStateShutdown,
}
