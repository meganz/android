package mega.privacy.android.feature.photos.presentation.timeline

import mega.privacy.android.domain.entity.node.TypedFileNode
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * To avoid extra conversion from [TypedFileNode].modification to [java.time.LocalDateTime]
 */
object TimelineDateCache {

    private val zoneId = ZoneId.systemDefault()
    private val cache = ConcurrentHashMap<Long, ZonedDateTime>()

    /**
     * Returns a [ZonedDateTime] for the calendar day that contains [epoch].
     *
     * The returned value is truncated to the start of the day
     * (midnight in the system default time zone). If you need the exact
     * time-of-day for a node, compute it directly:
     * `Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault())`
     */
    fun get(epoch: Long): ZonedDateTime {
        val day = epochDay(epoch)
        return cache.getOrPut(day) {
            Instant.ofEpochSecond(epoch)
                .atZone(zoneId)
                .truncatedTo(ChronoUnit.DAYS)
        }
    }

    fun month(epoch: Long): Month = get(epoch).month

    fun epochDay(epoch: Long): Long =
        Instant.ofEpochSecond(epoch).atZone(zoneId).toLocalDate().toEpochDay()
}
