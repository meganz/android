package mega.privacy.android.domain.entity

/**
 * Battery Info
 */
data class BatteryInfo(
    /**
     * current battery level
     */
    val level: Int,
    /**
     * whether device is charging or not
     */
    val isCharging: Boolean,
)
