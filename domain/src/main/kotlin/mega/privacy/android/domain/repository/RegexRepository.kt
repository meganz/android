package mega.privacy.android.domain.repository

import java.util.regex.Pattern

/**
 * Regex repository
 *
 */
interface RegexRepository {
    /**
     * Get the regex pattern for web url
     *
     * @return regex pattern
     */
    val webUrlPattern: Pattern
}