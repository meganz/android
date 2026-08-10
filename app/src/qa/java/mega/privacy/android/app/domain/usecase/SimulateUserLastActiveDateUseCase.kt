package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.QASimulationRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// The offsets below are arbitrary QA-only values with no strict requirement; they just need to keep
// purge after warning and after the last active date for the simulation. They live at file level
// (not owned by either use case) so the simulate (forward) and get-previous (reverse) use cases
// share the same values and the mapping stays exactly invertible.

/** ~3 months, as a fixed 90-day offset. */
internal val PURGE_OFFSET_SECONDS = TimeUnit.DAYS.toSeconds(90)

/** ~2 months, as a fixed 60-day offset. */
internal val WARNING_OFFSET_SECONDS = TimeUnit.DAYS.toSeconds(60)

/**
 * QA only. Simulates the user's last active date by setting the dev option purge attribute
 * (USER_ATTR_DEV_OPT).
 *
 * Given the selected last active date, it derives (using fixed day offsets rather than calendar
 * months, so the offsets are exactly reversible for QA collision checks; month accuracy is not
 * required for testing):
 * - lastActiveTimestamp: the selected date.
 * - warningTimestamp: [WARNING_OFFSET_SECONDS] (~2 months) after the selected date.
 * - purgeTimestamp: [PURGE_OFFSET_SECONDS] (~3 months) after the selected date.
 * - reason: fixed value [PURGE_REASON].
 */
class SimulateUserLastActiveDateUseCase @Inject constructor(
    private val qaSimulationRepository: QASimulationRepository,
) {
    /**
     * Derives the purge schedule from the selected date and writes it via the QA simulation repository.
     *
     * @param lastActiveTimestamp the user's last active time, in epoch seconds.
     */
    suspend operator fun invoke(lastActiveTimestamp: Long) {
        qaSimulationRepository.setDevOptForPurge(
            purgeTimestamp = lastActiveTimestamp + PURGE_OFFSET_SECONDS,
            reason = PURGE_REASON,
            warningTimestamp = lastActiveTimestamp + WARNING_OFFSET_SECONDS,
            lastActiveTimestamp = lastActiveTimestamp,
        )
    }

    companion object {
        /**
         * Purge reason code, fixed to inactivity. Mirrors `PURGE_REASON_INACTIVE = 4` from the SDK
         * (`mega/types.h`, enum `PurgeReason`). The SDK does not export this enum to the Java layer
         * since this is only a simulated event, so the raw value is hardcoded here.
         */
        private const val PURGE_REASON = 4
    }
}
