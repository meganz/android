package mega.privacy.android.domain.usecase

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Formats the current UTC time for a log entry tag.
 *
 * Runs once per client app log line, so it is deliberately allocation light: the
 * [DateTimeFormatter] is immutable and thread safe, so it is built once for the
 * process and shared rather than held per instance.
 */
class GetCurrentTimeStringUseCase @Inject constructor() {

    operator fun invoke(): String = FORMATTER.format(Instant.now())

    private companion object {
        val FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneOffset.UTC)
    }
}
