package mega.privacy.android.app.presentation.view.extension

import mega.privacy.android.domain.entity.node.FileNode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun FileNode.formattedModifiedDate(locale: Locale): String {
    val dateFormat = SimpleDateFormat(
        android.text.format.DateFormat.getBestDateTimePattern(
            locale, "d MMM yyyy HH:mm"
        ),
        locale
    )
    val cal = Calendar.getInstance().apply {
        timeInMillis = modificationTime * 1000
    }
    return dateFormat.format(cal.time)
}
