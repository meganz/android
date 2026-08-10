package mega.privacy.android.feature.photos.presentation.timeline

import java.time.format.DateTimeFormatter
import java.util.Locale

object TimelineFormatters {
    val dayMonth: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMMM")
    val dayMonthYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMMM uuuu")
    val year: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu")
    val month: DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLLL", Locale.getDefault())
    val monthYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
}
