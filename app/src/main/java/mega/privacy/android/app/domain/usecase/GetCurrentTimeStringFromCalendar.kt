package mega.privacy.android.app.domain.usecase

import android.text.format.DateFormat
import mega.privacy.android.domain.usecase.GetCurrentTimeString
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class GetCurrentTimeStringFromCalendar @Inject constructor() : GetCurrentTimeString {
    override suspend fun invoke(format: String, timeZone: String) =
        DateFormat.format(format,
            Calendar.getInstance(TimeZone.getTimeZone(timeZone))).toString()
}