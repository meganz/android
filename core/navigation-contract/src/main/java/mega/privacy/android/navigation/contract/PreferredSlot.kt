package mega.privacy.android.navigation.contract

/**
 * Defines the preferred slot for a navigation item
 */
sealed interface PreferredSlot {
    /**
     * Specifies a specific slot number (1-based)
     */
    data class Ordered(val slot: Int) : PreferredSlot

    /**
     * Specifies that this item should be placed in the last available slot
     */
    data object Last : PreferredSlot
} 