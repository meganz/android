package mega.privacy.android.core.formatter

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatModifiedDate(locale: Locale, modificationTime: Long): String {
    val dateFormat = SimpleDateFormat(
        DateFormat.getBestDateTimePattern(
            locale, "d MMM yyyy HH:mm"
        ),
        locale
    )
    val cal = Calendar.getInstance().apply {
        timeInMillis = modificationTime * 1000
    }
    return dateFormat.format(cal.time)
}
