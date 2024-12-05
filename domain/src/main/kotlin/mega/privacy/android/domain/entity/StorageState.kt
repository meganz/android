package mega.privacy.android.domain.entity

import androidx.annotation.Keep

/**
 * Mega Storage States
 * Note: the enum values are defined in an order
 * from good to poor storage situation. Do not change the order.
 */
@Keep
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