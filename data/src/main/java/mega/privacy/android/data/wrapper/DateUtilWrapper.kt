package mega.privacy.android.data.wrapper

import java.time.LocalDateTime

/**
 * The interface for wrapping the static method regarding date
 */
interface DateUtilWrapper {

    /**
     * Wrapping fromEpoch function
     * @return LocalDateTime
     */
    fun fromEpoch(seconds: Long): LocalDateTime
}