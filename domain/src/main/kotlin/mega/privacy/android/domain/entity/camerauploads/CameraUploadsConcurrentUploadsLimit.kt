package mega.privacy.android.domain.entity.camerauploads

/**
 * Enum class to represent the concurrent uploads limit for CU based on the device conditions
 *
 * @param limit The concurrent uploads limit
 */
enum class CameraUploadsConcurrentUploadsLimit(val limit: Int) {
    /**
     * Default limit
     */
    Default(8),

    /**
     * Limit when the thermal state is moderate
     */
    ThermalStateModerate(2),

    /**
     * Limit when the thermal state is severe
     */
    ThermalStateSevere(1),

    /**
     * Limit when the battery level is over 50%
     */
    BatteryLevelOver50(6),

    /**
     * Limit when the battery level is over 20%
     */
    BatteryLevelOver20(4),
}
