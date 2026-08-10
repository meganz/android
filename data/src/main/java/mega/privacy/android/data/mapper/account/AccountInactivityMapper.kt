package mega.privacy.android.data.mapper.account

import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.LastPurgeEvent
import mega.privacy.android.domain.entity.account.AccountInactivity
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Maps a [LastPurgeEvent] to [AccountInactivity].
 *
 * Returns null when the purge was not triggered by inactivity ([PURGE_REASON_INACTIVE]) or when the
 * event carries no last-active timestamp, since only inactive-reason purges drive the banner.
 */
internal class AccountInactivityMapper @Inject constructor(
    private val deviceGateway: DeviceGateway,
) {
    /**
     * @param event the last purge event to map.
     * @return the [AccountInactivity], or null if [event] is not an inactive-reason purge.
     */
    operator fun invoke(event: LastPurgeEvent): AccountInactivity? {
        val lastActiveTs = event.lastActiveTs
            ?.takeIf { event.reason == PURGE_REASON_INACTIVE }
            ?: return null
        val nowSeconds = deviceGateway.now / MILLIS_IN_SECOND
        return AccountInactivity(
            inactivityMonths = monthsBetween(lastActiveTs, nowSeconds),
            purgeTimestamp = event.ts,
        )
    }

    private fun monthsBetween(fromEpochSeconds: Long, toEpochSeconds: Long): Int {
        val from = Instant.ofEpochSecond(fromEpochSeconds).atZone(ZoneOffset.UTC)
        val to = Instant.ofEpochSecond(toEpochSeconds).atZone(ZoneOffset.UTC)
        return ChronoUnit.MONTHS.between(from, to).toInt().coerceAtLeast(1)
    }

    companion object {
        /**
         * Purge reason code for inactivity. Mirrors `PURGE_REASON_INACTIVE = 4` from the SDK
         * (`mega/types.h`, enum `PurgeReason`). The SDK does not export this enum to the Java
         * layer, so the raw value is hardcoded here. Only this reason carries `lastActiveTs`.
         */
        // Todo: Will be replaced with MegaApiJava constants in the next version
        private const val PURGE_REASON_INACTIVE = 4
        private const val MILLIS_IN_SECOND = 1000
    }
}
