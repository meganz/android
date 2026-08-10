package mega.privacy.android.feature.photos.presentation.timeline

import android.text.format.DateFormat.getBestDateTimePattern
import mega.privacy.android.feature.photos.model.TimelineGridSize
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object TimelineDateTextCache {

    private val formatterCache = ConcurrentHashMap<FormatterKey, DateTimeFormatter>()
    private val textCache = ConcurrentHashMap<TextKey, String>()

    fun get(
        epochSeconds: Long,
        gridSize: TimelineGridSize,
        locale: Locale,
    ): String {
        val dayKey = TimelineDateCache.epochDay(epochSeconds)
        val key = TextKey(dayKey, gridSize, locale.language)
        return textCache.getOrPut(key) {
            val zdt = TimelineDateCache.get(epochSeconds)
            val nowYear = Year.now().value
            val skeleton = when (gridSize) {
                TimelineGridSize.Large -> {
                    if (zdt.year == nowYear) "dd MMMM"
                    else "dd MMMM yyyy"
                }

                else -> {
                    if (zdt.year == nowYear) "LLLL"
                    else "LLLL yyyy"
                }
            }

            val pattern = getBestDateTimePattern(locale, skeleton)
            val formatter = formatterCache.getOrPut(FormatterKey(pattern, locale)) {
                DateTimeFormatter.ofPattern(pattern, locale)
            }
            formatter.format(zdt)
        }
    }

    private data class TextKey(
        val epochDay: Long,
        val gridSize: TimelineGridSize,
        val language: String,
    )

    private data class FormatterKey(
        val pattern: String,
        val locale: Locale,
    )
}
