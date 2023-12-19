package mega.privacy.android.data.mapper

import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * ISO6709LocationMapper is for parsing ISO 6709 formatted location strings
 * and extracting latitude and longitude coordinates.
 */
internal class ISO6709LocationMapper @Inject constructor() {

    /**
     * Parses the provided ISO 6709 formatted location string and extracts latitude and longitude coordinates.
     *
     * @param locationString The ISO 6709 formatted location string e.g. +51.528645-0.073989/ to be parsed.
     * Human Readable format e.g "40°12'13\"N 75°00'15\"W" or 40°12'N 75°00'W +23.23000 or "40°12'13\"N 75°00'15\"W"
     * formats are not parsed using this mapper
     * @return A Pair containing latitude and longitude coordinates, or null if parsing fails.
     */
    operator fun invoke(locationString: String): Pair<Double, Double>? = runCatching {
        val patternCoordinates: Pattern = Pattern.compile("([\\+\\-]\\d+\\.?\\d*)")
        val tokens = buildList {
            val matcherCoordinates = patternCoordinates.matcher(locationString)
            while (matcherCoordinates.find()) {
                val token = matcherCoordinates.group(1)
                token?.let {
                    if (token.startsWith("+") || token.startsWith("-")) {
                        val sign = if (token[0] == '+') 1 else -1
                        add(token.substring(1, token.length).toDoubleOrNull()?.times(sign))
                    } else {
                        add(token.toDoubleOrNull())
                    }
                }
            }
        }.filterNotNull()
        if (tokens.size < 2) {
            return null
        }
        val latitude = tokens[0]
        val longitude = tokens[1]
        return Pair(latitude, longitude)
    }.onFailure {
        Timber.e("ISO6709LocationMapper exception $it")
    }.getOrNull()
}
