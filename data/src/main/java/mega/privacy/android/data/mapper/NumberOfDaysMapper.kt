package mega.privacy.android.data.mapper

import mega.privacy.android.data.gateway.DeviceGateway
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * NumberOfDaysMapper
 * Mapper to compare 2 dates (the end date and current date) to get the difference
 * in number of days as [Long]
 */
class NumberOfDaysMapper @Inject constructor(
    private val deviceGateway: DeviceGateway
) {
    /**
     * Invoke
     * @param endTimeInMillis the time stamp of the supposedly end date
     * @return the number of day difference between the 2 dates.
     */
    operator fun invoke(
        endTimeInMillis: Long,
        startTimeInMillis: Long = deviceGateway.now,
    ): Long {
        val timeDifference = endTimeInMillis - startTimeInMillis
        return TimeUnit.MILLISECONDS.toDays(timeDifference)
    }
}