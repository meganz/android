package mega.privacy.android.domain.entity

/**
 * Mega Storage States
 */
enum class StorageState {

    /**
     * Unknown state
     */
    Unknown,

    /**
     * Green Storage State
     */
    Green,

    /**
     * Orange Storage State
     */
    Orange,

    /**
     * Red Storage State
     */
    Red,

    /**
     * Change Storage State
     */
    Change,

    /**
     * PayWall Storage State
     */
    PayWall,
}