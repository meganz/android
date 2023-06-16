package mega.privacy.android.data.mapper

import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * NumberOfDaysMapper
 * Mapper to compare 2 dates (the end date and current date) to get the difference
 * in number of days as [Long]
 */
class NumberOfDaysMapper @Inject constructor() {
    /**
     * Invoke
     * @param timeStampInMillis the time stamp of the supposedly end date
     * @return the number of day difference between the 2 dates.
     */
    operator fun invoke(timeStampInMillis: Long): Long {
        val endTime = Calendar.getInstance().apply {
            timeInMillis = timeStampInMillis
        }
        val timeDifference = Calendar.getInstance().let {
            endTime.timeInMillis - it.timeInMillis
        }
        return timeDifference / TimeUnit.DAYS.toMillis(1)
    }
}